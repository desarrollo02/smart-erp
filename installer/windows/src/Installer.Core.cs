using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Management;
using System.Net;
using System.Net.NetworkInformation;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Text.RegularExpressions;
using System.Web.Script.Serialization;
using Microsoft.Win32;

namespace Logixone.Installer
{
    internal enum CompatibilityStatus
    {
        Compatible = 0,
        CompatibleWithWarnings = 1,
        Blocked = 2
    }

    internal enum CheckLevel
    {
        Pass,
        Information,
        Warning,
        Blocker
    }

    internal enum PortOwnership
    {
        Free,
        Logixone,
        Other
    }

    internal sealed class CheckResult
    {
        public string Code { get; set; }
        public string Title { get; set; }
        public CheckLevel Level { get; set; }
        public string Actual { get; set; }
        public string Requirement { get; set; }
        public string Remediation { get; set; }
    }

    internal sealed class PreflightReport
    {
        public string InstallerVersion { get; set; }
        public string Story { get; set; }
        public string Profile { get; set; }
        public string ApplicationDigest { get; set; }
        public string MigratorDigest { get; set; }
        public string InstallationDirectory { get; set; }
        public DateTimeOffset EvaluatedAt { get; set; }
        public CompatibilityStatus Status { get; set; }
        public IList<CheckResult> Checks { get; set; }
    }

    internal sealed class InstallerConfiguration
    {
        public string Product { get; set; }
        public string InstallerVersion { get; set; }
        public string Sprint { get; set; }
        public string Story { get; set; }
        public string Profile { get; set; }
        public string ReleaseChannel { get; set; }
        public string ApplicationImage { get; set; }
        public string ApplicationDigest { get; set; }
        public string MigratorImage { get; set; }
        public string MigratorDigest { get; set; }
        public int MinimumWindowsBuild { get; set; }
        public long MinimumMemoryBytes { get; set; }
        public long RecommendedMemoryBytes { get; set; }
        public long MinimumDiskBytes { get; set; }
        public long MinimumUpdateDiskBytes { get; set; }
        public long RecommendedDiskBytes { get; set; }
        public Version MinimumWslVersion { get; set; }
        public Version MinimumDockerEngineVersion { get; set; }
        public Version MinimumComposeVersion { get; set; }
        public string DockerDesktopInstallVersion { get; set; }
        public string DockerDownloadUrl { get; set; }
        public long DockerDownloadBytes { get; set; }
        public string DockerSha256 { get; set; }
        public string DockerLicenseName { get; set; }
        public string DockerLicenseUrl { get; set; }
        public IList<int> Ports { get; set; }
        public string DefaultInstallationDirectory { get; set; }
        public string ComposeProject { get; set; }
        public string LivenessUrl { get; set; }
        public string ReadinessUrl { get; set; }
        public string ApplicationUrl { get; set; }
        public string DataPolicy { get; set; }
        public IList<string> GeneratedFiles { get; set; }
        public string PackageDirectory { get; set; }
    }

    internal static class ManifestLoader
    {
        private const long GiB = 1024L * 1024L * 1024L;

        public static InstallerConfiguration Load(string path)
        {
            if (String.IsNullOrWhiteSpace(path))
            {
                throw new ArgumentException("Debe indicar el manifiesto.", "path");
            }

            string fullPath = Path.GetFullPath(path);
            string json = File.ReadAllText(fullPath, System.Text.Encoding.UTF8);
            var serializer = new JavaScriptSerializer();
            var root = AsMap(serializer.DeserializeObject(json), "root");
            var installer = Child(root, "installer");
            var baseline = Child(root, "baseline");
            var requirements = Child(root, "requirements");
            var windows = Child(requirements, "windows");
            var memory = Child(requirements, "memoryGiB");
            var disk = Child(requirements, "diskGiB");
            var wsl = Child(requirements, "wsl");
            var docker = Child(requirements, "docker");
            var installation = Child(root, "installation");
            var health = Child(installation, "health");

            var configuration = new InstallerConfiguration
            {
                Product = Text(installer, "product"),
                InstallerVersion = Text(installer, "version"),
                Sprint = Text(installer, "sprint"),
                Story = Text(installer, "story"),
                Profile = Text(installer, "profile"),
                ReleaseChannel = Text(installer, "releaseChannel"),
                ApplicationImage = Text(baseline, "applicationImage"),
                ApplicationDigest = Text(baseline, "applicationDigest"),
                MigratorImage = Text(baseline, "migratorImage"),
                MigratorDigest = Text(baseline, "migratorDigest"),
                MinimumWindowsBuild = Integer(windows, "minimumBuild"),
                MinimumMemoryBytes = Integer(memory, "minimum") * GiB,
                RecommendedMemoryBytes = Integer(memory, "recommended") * GiB,
                MinimumDiskBytes = Integer(disk, "minimum") * GiB,
                MinimumUpdateDiskBytes = Integer(disk, "updateMinimum") * GiB,
                RecommendedDiskBytes = Integer(disk, "recommended") * GiB,
                MinimumWslVersion = ParseVersion(Text(wsl, "minimumVersion"), "WSL"),
                MinimumDockerEngineVersion = ParseVersion(Text(docker, "minimumEngineVersion"), "Docker Engine"),
                MinimumComposeVersion = ParseVersion(Text(docker, "minimumComposeVersion"), "Docker Compose"),
                DockerDesktopInstallVersion = Text(docker, "desktopInstallVersion"),
                DockerDownloadUrl = Text(docker, "downloadUrl"),
                DockerDownloadBytes = LongInteger(docker, "downloadBytes"),
                DockerSha256 = Text(docker, "sha256").ToLowerInvariant(),
                DockerLicenseName = Text(docker, "licenseName"),
                DockerLicenseUrl = Text(docker, "licenseUrl"),
                Ports = IntegerList(requirements, "ports"),
                DefaultInstallationDirectory =
                    Environment.ExpandEnvironmentVariables(Text(installation, "defaultDirectory")),
                ComposeProject = Text(installation, "composeProject"),
                LivenessUrl = Text(health, "liveness"),
                ReadinessUrl = Text(health, "readiness"),
                ApplicationUrl = Text(health, "application"),
                DataPolicy = Text(installation, "dataPolicy"),
                GeneratedFiles = StringList(root, "generatedFiles"),
                PackageDirectory = Path.GetDirectoryName(fullPath)
            };

            Validate(configuration);
            return configuration;
        }

        private static void Validate(InstallerConfiguration configuration)
        {
            if (configuration.Story != "J11-S8-08")
            {
                throw new InvalidDataException("El manifiesto no pertenece a J11-S8-08.");
            }
            if (!Regex.IsMatch(configuration.ApplicationDigest ?? String.Empty, "^sha256:[0-9a-f]{64}$")
                || !Regex.IsMatch(configuration.MigratorDigest ?? String.Empty, "^sha256:[0-9a-f]{64}$"))
            {
                throw new InvalidDataException("Los digests del baseline no son SHA-256 válidos.");
            }
            if (!Regex.IsMatch(configuration.DockerSha256 ?? String.Empty, "^[0-9a-f]{64}$"))
            {
                throw new InvalidDataException("El hash de Docker Desktop no es SHA-256 válido.");
            }
            Uri ignored;
            if (!Uri.TryCreate(configuration.DockerDownloadUrl, UriKind.Absolute, out ignored)
                || ignored.Scheme != Uri.UriSchemeHttps)
            {
                throw new InvalidDataException("La descarga de Docker Desktop debe usar HTTPS.");
            }
            if (configuration.MinimumWindowsBuild < 26100
                || configuration.MinimumMemoryBytes <= 0
                || configuration.MinimumDiskBytes <= 0
                || configuration.MinimumUpdateDiskBytes <= 0
                || configuration.MinimumUpdateDiskBytes >= configuration.MinimumDiskBytes
                || configuration.DockerDownloadBytes <= 0
                || configuration.Ports.Count == 0
                || configuration.GeneratedFiles.Count == 0)
            {
                throw new InvalidDataException("La matriz de requisitos está incompleta.");
            }
            if (!String.Equals(configuration.DataPolicy, "PRESERVE_VOLUMES", StringComparison.Ordinal))
            {
                throw new InvalidDataException("La política de datos debe preservar volúmenes.");
            }
        }

        private static Dictionary<string, object> AsMap(object value, string name)
        {
            var map = value as Dictionary<string, object>;
            if (map == null)
            {
                throw new InvalidDataException("La sección '" + name + "' no es un objeto.");
            }
            return map;
        }

        private static Dictionary<string, object> Child(Dictionary<string, object> parent, string name)
        {
            object value;
            if (!parent.TryGetValue(name, out value))
            {
                throw new InvalidDataException("Falta la sección '" + name + "'.");
            }
            return AsMap(value, name);
        }

        private static string Text(Dictionary<string, object> map, string name)
        {
            object value;
            string text;
            if (!map.TryGetValue(name, out value)
                || String.IsNullOrWhiteSpace(text = Convert.ToString(value, CultureInfo.InvariantCulture)))
            {
                throw new InvalidDataException("Falta el valor '" + name + "'.");
            }
            return text;
        }

        private static int Integer(Dictionary<string, object> map, string name)
        {
            object value;
            int result;
            if (!map.TryGetValue(name, out value)
                || !Int32.TryParse(Convert.ToString(value, CultureInfo.InvariantCulture),
                    NumberStyles.Integer, CultureInfo.InvariantCulture, out result))
            {
                throw new InvalidDataException("El valor '" + name + "' no es entero.");
            }
            return result;
        }

        private static long LongInteger(Dictionary<string, object> map, string name)
        {
            object value;
            long result;
            if (!map.TryGetValue(name, out value)
                || !Int64.TryParse(Convert.ToString(value, CultureInfo.InvariantCulture),
                    NumberStyles.Integer, CultureInfo.InvariantCulture, out result))
            {
                throw new InvalidDataException("El valor '" + name + "' no es entero.");
            }
            return result;
        }

        private static IList<int> IntegerList(Dictionary<string, object> map, string name)
        {
            object value;
            var raw = map.TryGetValue(name, out value) ? value as object[] : null;
            if (raw == null)
            {
                throw new InvalidDataException("El valor '" + name + "' no es una lista.");
            }
            return raw.Select(item => Convert.ToInt32(item, CultureInfo.InvariantCulture)).ToList();
        }

        private static IList<string> StringList(Dictionary<string, object> map, string name)
        {
            object value;
            var raw = map.TryGetValue(name, out value) ? value as object[] : null;
            if (raw == null)
            {
                throw new InvalidDataException("El valor '" + name + "' no es una lista.");
            }
            return raw.Select(item => Convert.ToString(item, CultureInfo.InvariantCulture)).ToList();
        }

        private static Version ParseVersion(string text, string label)
        {
            Version version;
            if (!Version.TryParse(NormalizeVersion(text), out version))
            {
                throw new InvalidDataException("La versión de " + label + " es inválida.");
            }
            return version;
        }

        internal static string NormalizeVersion(string value)
        {
            if (String.IsNullOrWhiteSpace(value))
            {
                return String.Empty;
            }
            Match match = Regex.Match(value, @"\d+(?:\.\d+){1,3}");
            return match.Success ? match.Value : String.Empty;
        }
    }

    internal sealed class SystemSnapshot
    {
        public bool IsWindows { get; set; }
        public bool Is64BitOperatingSystem { get; set; }
        public string WindowsProductName { get; set; }
        public string WindowsDisplayVersion { get; set; }
        public int WindowsBuild { get; set; }
        public long TotalMemoryBytes { get; set; }
        public long FreeDiskBytes { get; set; }
        public bool VirtualizationKnown { get; set; }
        public bool VirtualizationEnabled { get; set; }
        public bool SlatKnown { get; set; }
        public bool SlatEnabled { get; set; }
        public bool WslAvailable { get; set; }
        public Version WslVersion { get; set; }
        public bool DockerCliAvailable { get; set; }
        public Version DockerEngineVersion { get; set; }
        public bool DockerDaemonAvailable { get; set; }
        public Version ComposeVersion { get; set; }
        public bool PendingReboot { get; set; }
        public bool NetworkProbed { get; set; }
        public bool NetworkAvailable { get; set; }
        public bool IsAdministrator { get; set; }
        public bool PreviousInstallation { get; set; }
        public bool ExistingVolumes { get; set; }
        public string ExistingConfigurationSource { get; set; }
        public string InstallationDirectory { get; set; }
        public IDictionary<int, PortOwnership> PortOwnership { get; set; }
    }

    internal static class PreflightEvaluator
    {
        private const double GiB = 1024d * 1024d * 1024d;

        public static PreflightReport Evaluate(
            InstallerConfiguration configuration,
            SystemSnapshot snapshot)
        {
            if (configuration == null) throw new ArgumentNullException("configuration");
            if (snapshot == null) throw new ArgumentNullException("snapshot");

            var checks = new List<CheckResult>();
            Add(checks, "windows", "Sistema operativo",
                !snapshot.IsWindows || snapshot.WindowsBuild < configuration.MinimumWindowsBuild
                    ? CheckLevel.Blocker : CheckLevel.Pass,
                snapshot.WindowsProductName + " " + snapshot.WindowsDisplayVersion
                    + " (build " + snapshot.WindowsBuild + ")",
                "Windows 11 x64 build " + configuration.MinimumWindowsBuild + " o superior",
                "Actualice a una edición soportada de Windows 11.");

            Add(checks, "architecture", "Arquitectura",
                snapshot.Is64BitOperatingSystem ? CheckLevel.Pass : CheckLevel.Blocker,
                snapshot.Is64BitOperatingSystem ? "x64" : "no x64",
                "x64",
                "Use una máquina Windows x64.");

            AddRangeCheck(checks, "memory", "Memoria RAM", snapshot.TotalMemoryBytes,
                configuration.MinimumMemoryBytes, configuration.RecommendedMemoryBytes,
                "Cierre aplicaciones o amplíe la memoria asignada a la VM.");
            bool updateOrRepair = snapshot.PreviousInstallation || snapshot.ExistingVolumes;
            AddRangeCheck(checks, "disk", "Espacio libre", snapshot.FreeDiskBytes,
                updateOrRepair ? configuration.MinimumUpdateDiskBytes : configuration.MinimumDiskBytes,
                configuration.RecommendedDiskBytes,
                "Libere espacio en la unidad elegida sin borrar volúmenes de Logixone.");

            AddKnownBoolean(checks, "virtualization", "Virtualización de firmware",
                snapshot.VirtualizationKnown, snapshot.VirtualizationEnabled,
                "Habilitada", "Habilite virtualización en BIOS/UEFI.");
            AddKnownBoolean(checks, "slat", "SLAT",
                snapshot.SlatKnown, snapshot.SlatEnabled,
                "Disponible", "Use una CPU/VM que exponga SLAT.");

            CheckLevel wslLevel = !snapshot.WslAvailable || snapshot.WslVersion == null
                ? CheckLevel.Warning
                : (snapshot.WslVersion < configuration.MinimumWslVersion
                    ? CheckLevel.Warning : CheckLevel.Pass);
            Add(checks, "wsl", "WSL",
                wslLevel,
                snapshot.WslAvailable && snapshot.WslVersion != null
                    ? snapshot.WslVersion.ToString() : "No disponible",
                configuration.MinimumWslVersion + " o superior",
                "El plan propondrá instalar o actualizar WSL después del consentimiento.");

            CheckLevel dockerLevel = !snapshot.DockerCliAvailable
                ? CheckLevel.Warning
                : (!snapshot.DockerDaemonAvailable
                    ? CheckLevel.Warning
                    : (snapshot.DockerEngineVersion == null
                        || snapshot.DockerEngineVersion < configuration.MinimumDockerEngineVersion
                        ? CheckLevel.Warning : CheckLevel.Pass));
            Add(checks, "docker", "Docker Engine",
                dockerLevel,
                snapshot.DockerEngineVersion == null
                    ? (snapshot.DockerCliAvailable ? "CLI presente; motor no disponible" : "No instalado")
                    : snapshot.DockerEngineVersion.ToString(),
                configuration.MinimumDockerEngineVersion + " o superior",
                "Se reutilizará una versión compatible o se propondrá Docker Desktop "
                    + configuration.DockerDesktopInstallVersion + ".");

            CheckLevel composeLevel = snapshot.ComposeVersion == null
                || snapshot.ComposeVersion < configuration.MinimumComposeVersion
                ? CheckLevel.Warning : CheckLevel.Pass;
            Add(checks, "compose", "Docker Compose",
                composeLevel,
                snapshot.ComposeVersion == null ? "No disponible" : snapshot.ComposeVersion.ToString(),
                configuration.MinimumComposeVersion + " o superior",
                "Docker Desktop incluye la versión de Compose aprobada.");

            foreach (int port in configuration.Ports)
            {
                PortOwnership ownership;
                if (!snapshot.PortOwnership.TryGetValue(port, out ownership))
                {
                    ownership = PortOwnership.Free;
                }
                CheckLevel level = ownership == PortOwnership.Other
                    ? CheckLevel.Blocker
                    : (ownership == PortOwnership.Logixone ? CheckLevel.Information : CheckLevel.Pass);
                Add(checks, "port-" + port, "Puerto " + port,
                    level,
                    ownership == PortOwnership.Free ? "Libre"
                        : (ownership == PortOwnership.Logixone ? "Usado por Logixone" : "Ocupado por otro proceso"),
                    "Libre o perteneciente al proyecto Compose '" + configuration.ComposeProject + "'",
                    "Libere o cambie el puerto antes de instalar.");
            }

            Add(checks, "network", "Red y TLS",
                !snapshot.NetworkProbed ? CheckLevel.Information
                    : (snapshot.NetworkAvailable ? CheckLevel.Pass : CheckLevel.Warning),
                !snapshot.NetworkProbed ? "No ejecutado en este modo controlado"
                    : (snapshot.NetworkAvailable ? "Origen oficial accesible" : "No confirmado"),
                "Acceso HTTPS a los orígenes declarados",
                "Revise proxy, DNS, TLS y políticas corporativas.");

            Add(checks, "reboot", "Reinicio pendiente",
                snapshot.PendingReboot ? CheckLevel.Warning : CheckLevel.Pass,
                snapshot.PendingReboot ? "Detectado" : "No detectado",
                "Sin reinicio pendiente recomendado",
                "Reinicie Windows antes de continuar si la política de su organización lo exige.");

            Add(checks, "permissions", "Permisos actuales",
                CheckLevel.Information,
                snapshot.IsAdministrator ? "Administrador" : "Usuario estándar",
                "El preflight no necesita elevación",
                "UAC se solicitará solo si WSL u otra característica realmente lo requiere.");

            bool orphanVolumes = snapshot.ExistingVolumes
                && !snapshot.PreviousInstallation
                && String.IsNullOrWhiteSpace(snapshot.ExistingConfigurationSource);
            Add(checks, "previous-installation", "Instalación previa",
                orphanVolumes ? CheckLevel.Blocker
                    : (snapshot.PreviousInstallation || snapshot.ExistingVolumes
                        ? CheckLevel.Information : CheckLevel.Pass),
                snapshot.PreviousInstallation
                    ? "Edición previa detectada"
                    : (snapshot.ExistingVolumes
                        ? "Volúmenes y configuración adoptable detectados" : "No detectada"),
                "Actualizar o reparar sin eliminar datos",
                orphanVolumes
                    ? "Seleccione la configuración previa que corresponde a esos volúmenes."
                    : "El instalador preservará configuración y volúmenes.");

            CheckLevel pathLevel = ValidateInstallationPath(snapshot.InstallationDirectory)
                ? CheckLevel.Pass : CheckLevel.Blocker;
            Add(checks, "path", "Ruta de instalación",
                pathLevel,
                snapshot.InstallationDirectory,
                "Ruta absoluta local, menor de 180 caracteres",
                "Elija una ruta local más corta y válida.");

            CompatibilityStatus status = checks.Any(item => item.Level == CheckLevel.Blocker)
                ? CompatibilityStatus.Blocked
                : (checks.Any(item => item.Level == CheckLevel.Warning)
                    ? CompatibilityStatus.CompatibleWithWarnings
                    : CompatibilityStatus.Compatible);

            return new PreflightReport
            {
                InstallerVersion = configuration.InstallerVersion,
                Story = configuration.Story,
                Profile = configuration.Profile,
                ApplicationDigest = configuration.ApplicationDigest,
                MigratorDigest = configuration.MigratorDigest,
                InstallationDirectory = snapshot.InstallationDirectory,
                EvaluatedAt = DateTimeOffset.Now,
                Status = status,
                Checks = checks
            };
        }

        private static bool ValidateInstallationPath(string path)
        {
            if (String.IsNullOrWhiteSpace(path) || path.Length >= 180)
            {
                return false;
            }
            try
            {
                if (!Path.IsPathRooted(path))
                {
                    return false;
                }
                string fullPath = Path.GetFullPath(path);
                return fullPath.IndexOfAny(Path.GetInvalidPathChars()) < 0
                    && !String.Equals(Path.GetPathRoot(fullPath), fullPath, StringComparison.OrdinalIgnoreCase);
            }
            catch
            {
                return false;
            }
        }

        private static void AddRangeCheck(
            IList<CheckResult> checks,
            string code,
            string title,
            long actual,
            long minimum,
            long recommended,
            string remediation)
        {
            CheckLevel level = actual < 0
                ? CheckLevel.Warning
                : (actual < minimum ? CheckLevel.Blocker
                    : (actual < recommended ? CheckLevel.Warning : CheckLevel.Pass));
            Add(checks, code, title, level,
                actual < 0 ? "No disponible" : (actual / GiB).ToString("0.0", CultureInfo.InvariantCulture) + " GiB",
                "mínimo " + (minimum / GiB).ToString("0", CultureInfo.InvariantCulture)
                    + " GiB; recomendado " + (recommended / GiB).ToString("0", CultureInfo.InvariantCulture) + " GiB",
                remediation);
        }

        private static void AddKnownBoolean(
            IList<CheckResult> checks,
            string code,
            string title,
            bool known,
            bool value,
            string requirement,
            string remediation)
        {
            Add(checks, code, title,
                !known ? CheckLevel.Warning : (value ? CheckLevel.Pass : CheckLevel.Blocker),
                !known ? "No se pudo determinar" : (value ? "Disponible" : "No disponible"),
                requirement,
                remediation);
        }

        private static void Add(
            IList<CheckResult> checks,
            string code,
            string title,
            CheckLevel level,
            string actual,
            string requirement,
            string remediation)
        {
            checks.Add(new CheckResult
            {
                Code = code,
                Title = title,
                Level = level,
                Actual = actual ?? String.Empty,
                Requirement = requirement,
                Remediation = remediation
            });
        }
    }

    internal static class WindowsSystemProbe
    {
        public static SystemSnapshot Capture(
            InstallerConfiguration configuration,
            string installationDirectory,
            bool probeNetwork)
        {
            var snapshot = new SystemSnapshot
            {
                IsWindows = RuntimeInformation.IsOSPlatform(OSPlatform.Windows),
                Is64BitOperatingSystem = Environment.Is64BitOperatingSystem,
                InstallationDirectory = Path.GetFullPath(installationDirectory),
                PortOwnership = new Dictionary<int, PortOwnership>()
            };

            CaptureWindowsVersion(snapshot);
            CaptureHardware(snapshot);
            snapshot.FreeDiskBytes = GetFreeDiskBytes(snapshot.InstallationDirectory);
            CaptureWsl(snapshot);
            CaptureDocker(snapshot, configuration);
            if (snapshot.DockerDaemonAvailable)
            {
                // Un motor Linux operativo prueba que el backend ya dispone de
                // virtualización y SLAT aunque WMI los oculte al host invitado.
                snapshot.VirtualizationKnown = true;
                snapshot.VirtualizationEnabled = true;
                snapshot.SlatKnown = true;
                snapshot.SlatEnabled = true;
            }
            snapshot.PendingReboot = HasPendingReboot();
            snapshot.NetworkProbed = probeNetwork;
            snapshot.NetworkAvailable = probeNetwork && CanReach(configuration.DockerDownloadUrl);
            snapshot.IsAdministrator = IsAdministrator();
            snapshot.PreviousInstallation =
                File.Exists(Path.Combine(snapshot.InstallationDirectory, "install-state.json"));
            snapshot.ExistingVolumes = snapshot.DockerDaemonAvailable
                && !String.IsNullOrWhiteSpace(Run("docker",
                    "volume ls --filter label=com.docker.compose.project="
                    + configuration.ComposeProject + " --format \"{{.Name}}\"", 5000).Output);
            snapshot.ExistingConfigurationSource = FindExistingConfigurationSource();
            foreach (int port in configuration.Ports)
            {
                snapshot.PortOwnership[port] = GetPortOwnership(
                    port, configuration.ComposeProject, snapshot.DockerDaemonAvailable);
            }
            return snapshot;
        }

        private static string FindExistingConfigurationSource()
        {
            try
            {
                string candidate = Environment.CurrentDirectory;
                string secretRoot = Path.Combine(candidate, ".tools", "secrets");
                string[] names =
                {
                    "postgres-password.txt",
                    "keycloak-admin-password.txt",
                    "oidc-client-secret.txt",
                    "demo-user-password.txt"
                };
                return names.All(name => File.Exists(Path.Combine(secretRoot, name)))
                    ? candidate : null;
            }
            catch
            {
                return null;
            }
        }

        private static void CaptureWindowsVersion(SystemSnapshot snapshot)
        {
            snapshot.WindowsProductName = Environment.OSVersion.Platform.ToString();
            snapshot.WindowsDisplayVersion = String.Empty;
            snapshot.WindowsBuild = Environment.OSVersion.Version.Build;
            try
            {
                using (RegistryKey key = RegistryKey.OpenBaseKey(
                    RegistryHive.LocalMachine, RegistryView.Registry64)
                    .OpenSubKey(@"SOFTWARE\Microsoft\Windows NT\CurrentVersion", false))
                {
                    if (key == null) return;
                    snapshot.WindowsProductName = Convert.ToString(
                        key.GetValue("ProductName"), CultureInfo.InvariantCulture);
                    snapshot.WindowsDisplayVersion = Convert.ToString(
                        key.GetValue("DisplayVersion"), CultureInfo.InvariantCulture);
                    int build;
                    if (Int32.TryParse(Convert.ToString(
                        key.GetValue("CurrentBuildNumber"), CultureInfo.InvariantCulture), out build))
                    {
                        snapshot.WindowsBuild = build;
                    }
                    if (snapshot.WindowsBuild >= 22000
                        && snapshot.WindowsProductName.StartsWith("Windows 10", StringComparison.OrdinalIgnoreCase))
                    {
                        snapshot.WindowsProductName = "Windows 11"
                            + snapshot.WindowsProductName.Substring("Windows 10".Length);
                    }
                }
            }
            catch
            {
                // El evaluador conserva el dato de Environment como fallback.
            }
        }

        private static void CaptureHardware(SystemSnapshot snapshot)
        {
            snapshot.TotalMemoryBytes = -1;
            try
            {
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT TotalPhysicalMemory FROM Win32_ComputerSystem"))
                {
                    foreach (ManagementObject item in searcher.Get())
                    {
                        snapshot.TotalMemoryBytes = Convert.ToInt64(
                            item["TotalPhysicalMemory"], CultureInfo.InvariantCulture);
                        break;
                    }
                }
            }
            catch
            {
                snapshot.TotalMemoryBytes = -1;
            }

            try
            {
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT VirtualizationFirmwareEnabled,SecondLevelAddressTranslationExtensions FROM Win32_Processor"))
                {
                    foreach (ManagementObject item in searcher.Get())
                    {
                        snapshot.VirtualizationKnown = item["VirtualizationFirmwareEnabled"] != null;
                        snapshot.VirtualizationEnabled = snapshot.VirtualizationKnown
                            && Convert.ToBoolean(item["VirtualizationFirmwareEnabled"], CultureInfo.InvariantCulture);
                        snapshot.SlatKnown = item["SecondLevelAddressTranslationExtensions"] != null;
                        snapshot.SlatEnabled = snapshot.SlatKnown
                            && Convert.ToBoolean(item["SecondLevelAddressTranslationExtensions"], CultureInfo.InvariantCulture);
                        break;
                    }
                }
            }
            catch
            {
                snapshot.VirtualizationKnown = false;
                snapshot.SlatKnown = false;
            }
        }

        private static long GetFreeDiskBytes(string installationDirectory)
        {
            try
            {
                string root = Path.GetPathRoot(Path.GetFullPath(installationDirectory));
                return new DriveInfo(root).AvailableFreeSpace;
            }
            catch
            {
                return -1;
            }
        }

        private static void CaptureWsl(SystemSnapshot snapshot)
        {
            CommandResult result = Run("wsl.exe", "--version", 5000);
            snapshot.WslAvailable = result.ExitCode == 0;
            string normalized = ManifestLoader.NormalizeVersion(result.Output);
            Version version;
            snapshot.WslVersion = Version.TryParse(normalized, out version) ? version : null;
        }

        private static void CaptureDocker(
            SystemSnapshot snapshot,
            InstallerConfiguration configuration)
        {
            CommandResult client = Run("docker.exe", "version --format \"{{.Client.Version}}\"", 5000);
            snapshot.DockerCliAvailable = client.ExitCode == 0;
            Version clientVersion;
            Version.TryParse(ManifestLoader.NormalizeVersion(client.Output), out clientVersion);

            CommandResult server = Run("docker.exe", "version --format \"{{.Server.Version}}\"", 5000);
            Version serverVersion = null;
            snapshot.DockerDaemonAvailable = server.ExitCode == 0
                && Version.TryParse(ManifestLoader.NormalizeVersion(server.Output), out serverVersion);
            snapshot.DockerEngineVersion = snapshot.DockerDaemonAvailable
                ? serverVersion : clientVersion;

            CommandResult compose = Run("docker.exe", "compose version --short", 5000);
            Version composeVersion = null;
            snapshot.ComposeVersion = compose.ExitCode == 0
                && Version.TryParse(ManifestLoader.NormalizeVersion(compose.Output), out composeVersion)
                ? composeVersion : null;
        }

        private static PortOwnership GetPortOwnership(int port, string composeProject, bool dockerAvailable)
        {
            bool active = IPGlobalProperties.GetIPGlobalProperties()
                .GetActiveTcpListeners().Any(endpoint => endpoint.Port == port);
            if (!active)
            {
                return PortOwnership.Free;
            }
            if (dockerAvailable)
            {
                CommandResult owner = Run("docker.exe",
                    "ps --filter \"label=com.docker.compose.project=" + composeProject
                    + "\" --filter \"publish=" + port + "\" --format \"{{.ID}}\"", 5000);
                if (owner.ExitCode == 0 && !String.IsNullOrWhiteSpace(owner.Output))
                {
                    return PortOwnership.Logixone;
                }
            }
            return PortOwnership.Other;
        }

        private static bool HasPendingReboot()
        {
            string[] keys =
            {
                @"SOFTWARE\Microsoft\Windows\CurrentVersion\Component Based Servicing\RebootPending",
                @"SOFTWARE\Microsoft\Windows\CurrentVersion\WindowsUpdate\Auto Update\RebootRequired"
            };
            foreach (string path in keys)
            {
                try
                {
                    using (RegistryKey key = RegistryKey.OpenBaseKey(
                        RegistryHive.LocalMachine, RegistryView.Registry64).OpenSubKey(path, false))
                    {
                        if (key != null) return true;
                    }
                }
                catch
                {
                    // Un registro no legible no autoriza un cambio; el diagnóstico sigue.
                }
            }
            return false;
        }

        private static bool CanReach(string url)
        {
            if (!NetworkInterface.GetIsNetworkAvailable())
            {
                return false;
            }
            try
            {
                var request = (HttpWebRequest)WebRequest.Create(url);
                request.Method = "HEAD";
                request.AllowAutoRedirect = true;
                request.Timeout = 5000;
                request.ReadWriteTimeout = 5000;
                request.UserAgent = "Logixone-Installer-Preflight";
                using (var response = (HttpWebResponse)request.GetResponse())
                {
                    return (int)response.StatusCode >= 200 && (int)response.StatusCode < 400;
                }
            }
            catch
            {
                return false;
            }
        }

        private static bool IsAdministrator()
        {
            try
            {
                var principal = new WindowsPrincipal(WindowsIdentity.GetCurrent());
                return principal.IsInRole(WindowsBuiltInRole.Administrator);
            }
            catch
            {
                return false;
            }
        }

        private static CommandResult Run(string fileName, string arguments, int timeoutMilliseconds)
        {
            try
            {
                var start = new ProcessStartInfo
                {
                    FileName = fileName,
                    Arguments = arguments,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true
                };
                using (Process process = Process.Start(start))
                {
                    if (process == null)
                    {
                        return new CommandResult(-1, String.Empty);
                    }
                    string output = process.StandardOutput.ReadToEnd();
                    string error = process.StandardError.ReadToEnd();
                    if (!process.WaitForExit(timeoutMilliseconds))
                    {
                        try { process.Kill(); } catch { }
                        return new CommandResult(-1, output + Environment.NewLine + error);
                    }
                    return new CommandResult(process.ExitCode,
                        (output + Environment.NewLine + error).Replace("\0", String.Empty));
                }
            }
            catch
            {
                return new CommandResult(-1, String.Empty);
            }
        }

        private sealed class CommandResult
        {
            public CommandResult(int exitCode, string output)
            {
                ExitCode = exitCode;
                Output = output ?? String.Empty;
            }

            public int ExitCode { get; private set; }
            public string Output { get; private set; }
        }
    }
}
