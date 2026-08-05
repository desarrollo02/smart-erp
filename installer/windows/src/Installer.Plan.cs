using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace Logixone.Installer
{
    internal enum PlannedDisposition
    {
        Reuse,
        Install,
        Update,
        Execute,
        Verify
    }

    internal sealed class InstallationAction
    {
        public int Order { get; set; }
        public string Code { get; set; }
        public string Component { get; set; }
        public string Version { get; set; }
        public PlannedDisposition Disposition { get; set; }
        public string Description { get; set; }
        public bool RequiresDownload { get; set; }
        public long DownloadBytes { get; set; }
        public bool RequiresElevation { get; set; }
        public bool MayRequireRestart { get; set; }
        public string LicenseName { get; set; }
        public string LicenseUrl { get; set; }
    }

    internal sealed class InstallationPlan
    {
        public string InstallerVersion { get; set; }
        public string Story { get; set; }
        public string Profile { get; set; }
        public string ReleaseChannel { get; set; }
        public string InstallationDirectory { get; set; }
        public string ApplicationDigest { get; set; }
        public string MigratorDigest { get; set; }
        public bool RequiresElevation { get; set; }
        public bool MayRequireRestart { get; set; }
        public long TotalDownloadBytes { get; set; }
        public IList<int> Ports { get; set; }
        public IList<InstallationAction> Actions { get; set; }
    }

    internal static class InstallationPlanBuilder
    {
        public static InstallationPlan Build(
            InstallerConfiguration configuration,
            SystemSnapshot snapshot,
            PreflightReport report)
        {
            if (configuration == null) throw new ArgumentNullException("configuration");
            if (snapshot == null) throw new ArgumentNullException("snapshot");
            if (report == null) throw new ArgumentNullException("report");
            if (report.Status == CompatibilityStatus.Blocked)
            {
                throw new InvalidOperationException(
                    "Una máquina bloqueada no puede generar un plan ejecutable.");
            }

            var actions = new List<InstallationAction>();
            int order = 1;

            bool wslCompatible = snapshot.WslAvailable
                && snapshot.WslVersion != null
                && snapshot.WslVersion >= configuration.MinimumWslVersion;
            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "wsl",
                Component = "WSL",
                Version = configuration.MinimumWslVersion + "+",
                Disposition = wslCompatible ? PlannedDisposition.Reuse : PlannedDisposition.Update,
                Description = wslCompatible
                    ? "Reutilizar WSL sin modificar sus distribuciones."
                    : "Instalar o actualizar WSL; no crear una distribución adicional.",
                RequiresElevation = !wslCompatible,
                MayRequireRestart = !wslCompatible
            });

            bool dockerCompatible = snapshot.DockerDaemonAvailable
                && snapshot.DockerEngineVersion != null
                && snapshot.DockerEngineVersion >= configuration.MinimumDockerEngineVersion
                && snapshot.ComposeVersion != null
                && snapshot.ComposeVersion >= configuration.MinimumComposeVersion;
            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "docker",
                Component = "Docker Desktop",
                Version = dockerCompatible
                    ? snapshot.DockerEngineVersion.ToString()
                    : configuration.DockerDesktopInstallVersion,
                Disposition = dockerCompatible ? PlannedDisposition.Reuse : PlannedDisposition.Install,
                Description = dockerCompatible
                    ? "Reutilizar el motor y Compose compatibles; no cambiar configuración global."
                    : "Descargar, verificar e instalar Docker Desktop por usuario con backend WSL2.",
                RequiresDownload = !dockerCompatible,
                DownloadBytes = dockerCompatible ? 0 : configuration.DockerDownloadBytes,
                RequiresElevation = false,
                MayRequireRestart = false,
                LicenseName = configuration.DockerLicenseName,
                LicenseUrl = configuration.DockerLicenseUrl
            });

            string payloadPath = Path.Combine(configuration.PackageDirectory ?? String.Empty, "payload.zip");
            long payloadBytes = File.Exists(payloadPath) ? new FileInfo(payloadPath).Length : 0;
            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "payload",
                Component = "Logixone demo-local",
                Version = configuration.InstallerVersion,
                Disposition = snapshot.PreviousInstallation
                    ? PlannedDisposition.Update : PlannedDisposition.Install,
                Description = "Verificar y desplegar el payload en " + snapshot.InstallationDirectory
                    + "; preservar configuración local y datos.",
                RequiresDownload = false,
                DownloadBytes = payloadBytes
            });

            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "secrets",
                Component = "Secretos locales",
                Version = "generación local",
                Disposition = snapshot.PreviousInstallation
                    || !String.IsNullOrWhiteSpace(snapshot.ExistingConfigurationSource)
                    ? PlannedDisposition.Reuse : PlannedDisposition.Install,
                Description = snapshot.PreviousInstallation
                    ? "Reutilizar archivos existentes sin leerlos en logs."
                    : (!String.IsNullOrWhiteSpace(snapshot.ExistingConfigurationSource)
                        ? "Adoptar los archivos de la instalación detectada sin registrar su contenido."
                    : "Generar valores aleatorios en archivos locales; no incluirlos en el paquete."
                    )
            });

            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "images",
                Component = "Imágenes Logixone",
                Version = configuration.Profile,
                Disposition = PlannedDisposition.Verify,
                Description = "Construir o reutilizar y verificar "
                    + configuration.ApplicationImage + " y " + configuration.MigratorImage
                    + " contra sus digests congelados."
            });

            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "compose",
                Component = "Docker Compose",
                Version = configuration.ComposeProject,
                Disposition = PlannedDisposition.Execute,
                Description = "Ejecutar migrador y aplicación sin eliminar volúmenes; publicar solo en loopback."
            });

            actions.Add(new InstallationAction
            {
                Order = order++,
                Code = "health",
                Component = "Validación final",
                Version = "liveness/readiness",
                Disposition = PlannedDisposition.Verify,
                Description = "Comprobar liveness, readiness y la ruta visual antes de declarar éxito."
            });

            return new InstallationPlan
            {
                InstallerVersion = configuration.InstallerVersion,
                Story = configuration.Story,
                Profile = configuration.Profile,
                ReleaseChannel = configuration.ReleaseChannel,
                InstallationDirectory = snapshot.InstallationDirectory,
                ApplicationDigest = configuration.ApplicationDigest,
                MigratorDigest = configuration.MigratorDigest,
                RequiresElevation = actions.Any(item => item.RequiresElevation),
                MayRequireRestart = actions.Any(item => item.MayRequireRestart),
                TotalDownloadBytes = actions.Sum(item => item.RequiresDownload ? item.DownloadBytes : 0),
                Ports = new List<int>(configuration.Ports),
                Actions = actions
            };
        }
    }
}
