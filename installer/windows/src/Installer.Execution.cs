using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading;

namespace Logixone.Installer
{
    internal enum ExecutionOutcome
    {
        Succeeded,
        Failed,
        Cancelled
    }

    internal enum ActionOutcome
    {
        Installed,
        Reused,
        Updated,
        Executed,
        Verified,
        Failed
    }

    internal sealed class InstallationConsent
    {
        public bool Accepted { get; set; }
        public string PlanFingerprint { get; set; }
        public DateTimeOffset AcceptedAt { get; set; }
        public bool ThirdPartyLicensesAccepted { get; set; }
    }

    internal sealed class ExecutedAction
    {
        public int Order { get; set; }
        public string Code { get; set; }
        public string Component { get; set; }
        public ActionOutcome Outcome { get; set; }
        public string Detail { get; set; }
    }

    internal sealed class InstallationResult
    {
        public ExecutionOutcome Outcome { get; set; }
        public DateTimeOffset StartedAt { get; set; }
        public DateTimeOffset FinishedAt { get; set; }
        public string LogPath { get; set; }
        public string Error { get; set; }
        public IList<ExecutedAction> Actions { get; set; }
    }

    internal sealed class ExecutionProgress
    {
        public int Current { get; set; }
        public int Total { get; set; }
        public string Phase { get; set; }
        public string Message { get; set; }
    }

    internal interface IInstallationOperations
    {
        string Begin(InstallationPlan plan, InstallationConsent consent);
        void InstallOrUpdateWsl(InstallationAction action, CancellationToken cancellationToken);
        void InstallDocker(InstallerConfiguration configuration, InstallationAction action,
            CancellationToken cancellationToken);
        void DeployPayload(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken);
        void EnsureSecretsAndConfiguration(InstallerConfiguration configuration,
            InstallationPlan plan, InstallationAction action, CancellationToken cancellationToken);
        void EnsureImages(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken);
        void StartCompose(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken);
        void VerifyHealth(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken);
        void Complete(InstallerConfiguration configuration, InstallationPlan plan,
            InstallationResult result);
        void Fail(string safeMessage);
    }

    internal static class PlanFingerprint
    {
        public static string Calculate(InstallationPlan plan)
        {
            if (plan == null) throw new ArgumentNullException("plan");
            var canonical = new StringBuilder();
            canonical.Append(plan.InstallerVersion).Append('|')
                .Append(plan.Story).Append('|')
                .Append(plan.Profile).Append('|')
                .Append(plan.ReleaseChannel).Append('|')
                .Append(plan.InstallationDirectory).Append('|')
                .Append(plan.ApplicationDigest).Append('|')
                .Append(plan.MigratorDigest).Append('|')
                .Append(plan.RequiresElevation).Append('|')
                .Append(plan.MayRequireRestart).Append('|')
                .Append(plan.TotalDownloadBytes).Append('|');
            foreach (InstallationAction action in plan.Actions.OrderBy(item => item.Order))
            {
                canonical.Append(action.Order).Append(':')
                    .Append(action.Code).Append(':')
                    .Append(action.Component).Append(':')
                    .Append(action.Version).Append(':')
                    .Append(action.Disposition).Append(':')
                    .Append(action.RequiresDownload).Append(':')
                    .Append(action.DownloadBytes).Append(':')
                    .Append(action.RequiresElevation).Append(':')
                    .Append(action.MayRequireRestart).Append(':')
                    .Append(action.LicenseName).Append(':')
                    .Append(action.LicenseUrl).Append(';');
            }
            using (SHA256 sha = SHA256.Create())
            {
                byte[] digest = sha.ComputeHash(Encoding.UTF8.GetBytes(canonical.ToString()));
                return String.Concat(digest.Select(value => value.ToString("x2", CultureInfo.InvariantCulture)));
            }
        }
    }

    internal sealed class InstallationExecutor
    {
        private readonly IInstallationOperations operations;
        private readonly Action<ExecutionProgress> progress;

        public InstallationExecutor(
            IInstallationOperations operations,
            Action<ExecutionProgress> progress)
        {
            if (operations == null) throw new ArgumentNullException("operations");
            this.operations = operations;
            this.progress = progress ?? delegate { };
        }

        public InstallationResult Execute(
            InstallerConfiguration configuration,
            SystemSnapshot snapshot,
            PreflightReport report,
            InstallationPlan plan,
            InstallationConsent consent,
            CancellationToken cancellationToken)
        {
            ValidateBeforeFirstWrite(configuration, snapshot, report, plan, consent);
            cancellationToken.ThrowIfCancellationRequested();

            var result = new InstallationResult
            {
                Outcome = ExecutionOutcome.Failed,
                StartedAt = DateTimeOffset.Now,
                Actions = new List<ExecutedAction>()
            };

            try
            {
                result.LogPath = operations.Begin(plan, consent);
                foreach (InstallationAction action in plan.Actions.OrderBy(item => item.Order))
                {
                    cancellationToken.ThrowIfCancellationRequested();
                    progress(new ExecutionProgress
                    {
                        Current = action.Order,
                        Total = plan.Actions.Count,
                        Phase = action.Code,
                        Message = action.Component
                    });
                    ExecuteAction(configuration, plan, action, cancellationToken);
                    result.Actions.Add(new ExecutedAction
                    {
                        Order = action.Order,
                        Code = action.Code,
                        Component = action.Component,
                        Outcome = ToOutcome(action.Disposition),
                        Detail = action.Description
                    });
                }
                result.Outcome = ExecutionOutcome.Succeeded;
                result.FinishedAt = DateTimeOffset.Now;
                operations.Complete(configuration, plan, result);
                progress(new ExecutionProgress
                {
                    Current = plan.Actions.Count,
                    Total = plan.Actions.Count,
                    Phase = "complete",
                    Message = "Instalación verificada"
                });
                return result;
            }
            catch (OperationCanceledException)
            {
                result.Outcome = ExecutionOutcome.Cancelled;
                result.FinishedAt = DateTimeOffset.Now;
                result.Error = "Cancelado en un límite seguro; no se eliminaron datos ni volúmenes.";
                operations.Fail(result.Error);
                return result;
            }
            catch (Exception exception)
            {
                result.Outcome = ExecutionOutcome.Failed;
                result.FinishedAt = DateTimeOffset.Now;
                result.Error = SafeError(exception);
                operations.Fail(result.Error);
                return result;
            }
        }

        private void ExecuteAction(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            InstallationAction action,
            CancellationToken cancellationToken)
        {
            switch (action.Code)
            {
                case "wsl":
                    if (action.Disposition != PlannedDisposition.Reuse)
                    {
                        operations.InstallOrUpdateWsl(action, cancellationToken);
                    }
                    break;
                case "docker":
                    if (action.Disposition != PlannedDisposition.Reuse)
                    {
                        operations.InstallDocker(configuration, action, cancellationToken);
                    }
                    break;
                case "payload":
                    operations.DeployPayload(configuration, plan, cancellationToken);
                    break;
                case "secrets":
                    operations.EnsureSecretsAndConfiguration(
                        configuration, plan, action, cancellationToken);
                    break;
                case "images":
                    operations.EnsureImages(configuration, plan, cancellationToken);
                    break;
                case "compose":
                    operations.StartCompose(configuration, plan, cancellationToken);
                    break;
                case "health":
                    operations.VerifyHealth(configuration, plan, cancellationToken);
                    break;
                default:
                    throw new InvalidOperationException(
                        "El plan contiene una fase desconocida: " + action.Code);
            }
        }

        private static void ValidateBeforeFirstWrite(
            InstallerConfiguration configuration,
            SystemSnapshot snapshot,
            PreflightReport report,
            InstallationPlan plan,
            InstallationConsent consent)
        {
            if (configuration == null || snapshot == null || report == null || plan == null)
            {
                throw new ArgumentException("Falta el contexto verificado de instalación.");
            }
            if (report.Status == CompatibilityStatus.Blocked)
            {
                throw new InvalidOperationException("Una máquina bloqueada no puede modificarse.");
            }
            if (consent == null || !consent.Accepted)
            {
                throw new InvalidOperationException("Falta el consentimiento explícito.");
            }
            if (!consent.ThirdPartyLicensesAccepted
                && plan.Actions.Any(item => !String.IsNullOrWhiteSpace(item.LicenseName)))
            {
                throw new InvalidOperationException(
                    "Falta aceptar las licencias de terceros mostradas en el plan.");
            }
            string expected = PlanFingerprint.Calculate(plan);
            if (!String.Equals(expected, consent.PlanFingerprint, StringComparison.Ordinal))
            {
                throw new InvalidOperationException(
                    "El plan cambió después del consentimiento; debe revisarse nuevamente.");
            }
            if (!String.Equals(plan.ApplicationDigest, configuration.ApplicationDigest,
                    StringComparison.Ordinal)
                || !String.Equals(plan.MigratorDigest, configuration.MigratorDigest,
                    StringComparison.Ordinal)
                || !String.Equals(plan.InstallationDirectory, snapshot.InstallationDirectory,
                    StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidOperationException(
                    "El plan no coincide con el baseline o el destino diagnosticado.");
            }
        }

        private static ActionOutcome ToOutcome(PlannedDisposition disposition)
        {
            switch (disposition)
            {
                case PlannedDisposition.Reuse: return ActionOutcome.Reused;
                case PlannedDisposition.Install: return ActionOutcome.Installed;
                case PlannedDisposition.Update: return ActionOutcome.Updated;
                case PlannedDisposition.Execute: return ActionOutcome.Executed;
                default: return ActionOutcome.Verified;
            }
        }

        private static string SafeError(Exception exception)
        {
            string message = exception == null ? "Fallo no especificado." : exception.Message;
            if (String.IsNullOrWhiteSpace(message))
            {
                message = "Fallo no especificado.";
            }
            message = message.Replace("\r", " ").Replace("\n", " ");
            return message.Length > 500 ? message.Substring(0, 500) : message;
        }
    }
}

