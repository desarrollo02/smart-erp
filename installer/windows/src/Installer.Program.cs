using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace Logixone.Installer
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            try
            {
                if (args.Any(item => String.Equals(item, "--help", StringComparison.OrdinalIgnoreCase)
                    || String.Equals(item, "-h", StringComparison.OrdinalIgnoreCase)))
                {
                    PrintUsage();
                    return 0;
                }

                string manifestPath = ValueAfter(args, "--manifest")
                    ?? Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "installer-manifest.json");
                string requestedDirectory = ValueAfter(args, "--install-dir");
                bool json = args.Any(item => String.Equals(item, "--json", StringComparison.OrdinalIgnoreCase));
                bool noNetwork = args.Any(item => String.Equals(item, "--no-network", StringComparison.OrdinalIgnoreCase));
                bool includePlan = args.Any(item => String.Equals(item, "--plan", StringComparison.OrdinalIgnoreCase));
                bool execute = args.Any(item => String.Equals(item, "--execute", StringComparison.OrdinalIgnoreCase));
                bool preflightMode = args.Any(item => String.Equals(item, "--preflight", StringComparison.OrdinalIgnoreCase))
                    || json || includePlan || execute;
                bool uiSmoke = args.Any(item => String.Equals(item, "--ui-smoke", StringComparison.OrdinalIgnoreCase));

                InstallerConfiguration configuration = ManifestLoader.Load(manifestPath);
                string installationDirectory = String.IsNullOrWhiteSpace(requestedDirectory)
                    ? configuration.DefaultInstallationDirectory
                    : Environment.ExpandEnvironmentVariables(requestedDirectory);

                if (uiSmoke)
                {
                    int smokeResult = InstallerForm.SmokeTest(configuration);
                    Console.WriteLine(smokeResult == 0 ? "UI_SMOKE_OK" : "UI_SMOKE_FAILED");
                    return smokeResult;
                }
                if (!preflightMode)
                {
                    Application.EnableVisualStyles();
                    Application.SetCompatibleTextRenderingDefault(false);
                    Application.Run(new InstallerForm(configuration, installationDirectory, !noNetwork));
                    return 0;
                }

                SystemSnapshot snapshot = WindowsSystemProbe.Capture(
                    configuration, installationDirectory, !noNetwork);
                PreflightReport report = PreflightEvaluator.Evaluate(configuration, snapshot);

                InstallationPlan plan = (includePlan || execute)
                    && report.Status != CompatibilityStatus.Blocked
                    ? InstallationPlanBuilder.Build(configuration, snapshot, report)
                    : null;
                if (execute)
                {
                    if (plan == null)
                    {
                        if (json) PrintJson(report); else PrintHuman(report);
                        return (int)CompatibilityStatus.Blocked;
                    }
                    string acceptedFingerprint = ValueAfter(args, "--accept-plan");
                    bool licensesAccepted = args.Any(item => String.Equals(
                        item, "--accept-third-party-licenses", StringComparison.OrdinalIgnoreCase));
                    if (String.IsNullOrWhiteSpace(acceptedFingerprint))
                    {
                        throw new ArgumentException(
                            "--execute requiere --accept-plan con la huella exacta mostrada.");
                    }
                    if (!licensesAccepted)
                    {
                        throw new ArgumentException(
                            "--execute requiere --accept-third-party-licenses después de revisar el plan.");
                    }
                    var consent = new InstallationConsent
                    {
                        Accepted = !String.IsNullOrWhiteSpace(acceptedFingerprint),
                        PlanFingerprint = acceptedFingerprint,
                        ThirdPartyLicensesAccepted = licensesAccepted,
                        AcceptedAt = DateTimeOffset.Now
                    };
                    var executor = new InstallationExecutor(
                        new WindowsInstallationOperations(snapshot),
                        current =>
                        {
                            if (!json)
                            {
                                Console.WriteLine(
                                    current.Current + "/" + current.Total + " " + current.Message);
                            }
                        });
                    InstallationResult executionResult = executor.Execute(
                        configuration, snapshot, report, plan, consent,
                        System.Threading.CancellationToken.None);
                    if (json)
                    {
                        PrintExecutionJson(plan, executionResult);
                    }
                    else
                    {
                        PrintExecution(executionResult);
                    }
                    return executionResult.Outcome == ExecutionOutcome.Succeeded ? 0 : 3;
                }
                if (json && plan != null)
                {
                    PrintPlanJson(report, plan);
                }
                else if (json)
                {
                    PrintJson(report);
                }
                else
                {
                    PrintHuman(report);
                    if (plan != null)
                    {
                        PrintPlan(plan);
                    }
                }
                return (int)report.Status;
            }
            catch (ArgumentException exception)
            {
                Console.Error.WriteLine("Uso inválido: " + exception.Message);
                return 64;
            }
            catch (Exception exception)
            {
                Console.Error.WriteLine("El diagnóstico falló de forma cerrada: " + exception.Message);
                return 70;
            }
        }

        private static string ValueAfter(string[] args, string option)
        {
            for (int index = 0; index < args.Length; index++)
            {
                if (!String.Equals(args[index], option, StringComparison.OrdinalIgnoreCase))
                {
                    continue;
                }
                if (index + 1 >= args.Length || args[index + 1].StartsWith("--", StringComparison.Ordinal))
                {
                    throw new ArgumentException("Falta el valor de " + option + ".");
                }
                return args[index + 1];
            }
            return null;
        }

        private static void PrintJson(PreflightReport report)
        {
            var serializer = new JavaScriptSerializer();
            var result = new Dictionary<string, object>
            {
                { "status", StatusName(report.Status) },
                { "installerVersion", report.InstallerVersion },
                { "story", report.Story },
                { "profile", report.Profile },
                { "evaluatedAt", report.EvaluatedAt.ToString("o", CultureInfo.InvariantCulture) },
                { "applicationDigest", report.ApplicationDigest },
                { "migratorDigest", report.MigratorDigest },
                { "installationDirectory", report.InstallationDirectory },
                {
                    "checks",
                    report.Checks.Select(item => new Dictionary<string, object>
                    {
                        { "code", item.Code },
                        { "title", item.Title },
                        { "level", item.Level.ToString().ToUpperInvariant() },
                        { "actual", item.Actual },
                        { "requirement", item.Requirement },
                        { "remediation", item.Remediation }
                    }).ToArray()
                }
            };
            Console.WriteLine(serializer.Serialize(result));
        }

        private static void PrintHuman(PreflightReport report)
        {
            Console.WriteLine("Logixone " + report.InstallerVersion + " | " + report.Story);
            Console.WriteLine("Resultado: " + StatusName(report.Status));
            Console.WriteLine("Destino: " + report.InstallationDirectory);
            Console.WriteLine();
            foreach (CheckResult check in report.Checks)
            {
                Console.WriteLine(
                    "[{0}] {1}: {2}",
                    check.Level.ToString().ToUpperInvariant(),
                    check.Title,
                    check.Actual);
                if (check.Level == CheckLevel.Warning || check.Level == CheckLevel.Blocker)
                {
                    Console.WriteLine("  Requisito: " + check.Requirement);
                    Console.WriteLine("  Recuperación: " + check.Remediation);
                }
            }
            Console.WriteLine();
            Console.WriteLine(report.Status == CompatibilityStatus.Blocked
                ? "No se realizó ningún cambio. La instalación no puede continuar."
                : "Este diagnóstico no realizó cambios. Revise y acepte el plan antes de continuar.");
        }

        private static void PrintPlanJson(PreflightReport report, InstallationPlan plan)
        {
            var serializer = new JavaScriptSerializer();
            var result = new Dictionary<string, object>
            {
                { "status", StatusName(report.Status) },
                { "installerVersion", plan.InstallerVersion },
                { "story", plan.Story },
                { "profile", plan.Profile },
                { "releaseChannel", plan.ReleaseChannel },
                { "installationDirectory", plan.InstallationDirectory },
                { "applicationDigest", plan.ApplicationDigest },
                { "migratorDigest", plan.MigratorDigest },
                { "requiresElevation", plan.RequiresElevation },
                { "mayRequireRestart", plan.MayRequireRestart },
                { "totalDownloadBytes", plan.TotalDownloadBytes },
                { "planFingerprint", PlanFingerprint.Calculate(plan) },
                { "ports", plan.Ports },
                {
                    "actions",
                    plan.Actions.Select(item => new Dictionary<string, object>
                    {
                        { "order", item.Order },
                        { "code", item.Code },
                        { "component", item.Component },
                        { "version", item.Version },
                        { "disposition", item.Disposition.ToString().ToUpperInvariant() },
                        { "description", item.Description },
                        { "requiresDownload", item.RequiresDownload },
                        { "downloadBytes", item.DownloadBytes },
                        { "requiresElevation", item.RequiresElevation },
                        { "mayRequireRestart", item.MayRequireRestart },
                        { "licenseName", item.LicenseName ?? String.Empty },
                        { "licenseUrl", item.LicenseUrl ?? String.Empty }
                    }).ToArray()
                },
                {
                    "warnings",
                    report.Checks.Where(item => item.Level == CheckLevel.Warning)
                        .Select(item => item.Title + ": " + item.Actual).ToArray()
                }
            };
            Console.WriteLine(serializer.Serialize(result));
        }

        private static void PrintPlan(InstallationPlan plan)
        {
            Console.WriteLine("PLAN PROPUESTO");
            Console.WriteLine("Canal: " + plan.ReleaseChannel);
            Console.WriteLine("Descarga: " + plan.TotalDownloadBytes + " bytes");
            Console.WriteLine("Elevación: " + (plan.RequiresElevation ? "sí" : "no"));
            Console.WriteLine("Reinicio posible: " + (plan.MayRequireRestart ? "sí" : "no"));
            Console.WriteLine("Huella del plan: " + PlanFingerprint.Calculate(plan));
            foreach (InstallationAction action in plan.Actions)
            {
                Console.WriteLine(
                    "{0}. [{1}] {2} {3}: {4}",
                    action.Order,
                    action.Disposition.ToString().ToUpperInvariant(),
                    action.Component,
                    action.Version,
                    action.Description);
                if (!String.IsNullOrWhiteSpace(action.LicenseName))
                {
                    Console.WriteLine("   Licencia: " + action.LicenseName + " | " + action.LicenseUrl);
                }
            }
            Console.WriteLine("El plan aún no fue aceptado y no se realizó ningún cambio.");
        }

        private static void PrintExecutionJson(InstallationPlan plan, InstallationResult result)
        {
            var serializer = new JavaScriptSerializer();
            var value = new Dictionary<string, object>
            {
                { "outcome", result.Outcome.ToString().ToUpperInvariant() },
                { "installerVersion", plan.InstallerVersion },
                { "story", plan.Story },
                { "planFingerprint", PlanFingerprint.Calculate(plan) },
                { "startedAt", result.StartedAt.ToString("o", CultureInfo.InvariantCulture) },
                { "finishedAt", result.FinishedAt.ToString("o", CultureInfo.InvariantCulture) },
                { "logPath", result.LogPath ?? String.Empty },
                { "error", result.Error ?? String.Empty },
                {
                    "actions",
                    result.Actions.Select(item => new Dictionary<string, object>
                    {
                        { "order", item.Order },
                        { "code", item.Code },
                        { "component", item.Component },
                        { "outcome", item.Outcome.ToString().ToUpperInvariant() }
                    }).ToArray()
                }
            };
            Console.WriteLine(serializer.Serialize(value));
        }

        private static void PrintExecution(InstallationResult result)
        {
            Console.WriteLine("Resultado de instalación: " + result.Outcome.ToString().ToUpperInvariant());
            Console.WriteLine("Log: " + (result.LogPath ?? "no creado"));
            if (!String.IsNullOrWhiteSpace(result.Error))
            {
                Console.WriteLine("Recuperación: " + result.Error);
            }
        }

        internal static string StatusName(CompatibilityStatus status)
        {
            switch (status)
            {
                case CompatibilityStatus.Compatible:
                    return "COMPATIBLE";
                case CompatibilityStatus.CompatibleWithWarnings:
                    return "COMPATIBLE_CON_ADVERTENCIAS";
                default:
                    return "BLOQUEADA";
            }
        }

        private static void PrintUsage()
        {
            Console.WriteLine("Logixone Windows Installer");
            Console.WriteLine("  --manifest <ruta>      Manifiesto versionado");
            Console.WriteLine("  --install-dir <ruta>   Destino propuesto");
            Console.WriteLine("  --json                 Salida estructurada");
            Console.WriteLine("  --plan                 Incluye el plan propuesto si el equipo no está bloqueado");
            Console.WriteLine("  --execute              Ejecuta el plan después de consentimiento verificable");
            Console.WriteLine("  --accept-plan <sha256> Huella exacta obtenida con --plan");
            Console.WriteLine("  --accept-third-party-licenses  Confirma las licencias mostradas");
            Console.WriteLine("  --preflight            Ejecuta solo el diagnóstico");
            Console.WriteLine("  --ui-smoke             Verifica que la interfaz pueda construirse sin mostrarla");
            Console.WriteLine("  --no-network           Omite la prueba HTTPS, solo para pruebas controladas");
            Console.WriteLine("  --help                 Muestra esta ayuda");
        }
    }
}
