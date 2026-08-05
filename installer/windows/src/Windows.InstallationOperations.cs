using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Web.Script.Serialization;

namespace Logixone.Installer
{
    internal sealed class WindowsInstallationOperations : IInstallationOperations
    {
        private readonly SystemSnapshot snapshot;
        private string installationRoot;
        private string releaseDirectory;
        private string stateDirectory;
        private string logPath;

        public WindowsInstallationOperations(SystemSnapshot snapshot)
        {
            if (snapshot == null) throw new ArgumentNullException("snapshot");
            this.snapshot = snapshot;
        }

        public string Begin(InstallationPlan plan, InstallationConsent consent)
        {
            installationRoot = SafeInstallationRoot(plan.InstallationDirectory);
            stateDirectory = Path.Combine(installationRoot, "state");
            string logs = Path.Combine(installationRoot, "logs");
            Directory.CreateDirectory(logs);
            Directory.CreateDirectory(stateDirectory);
            logPath = Path.Combine(
                logs,
                "install-" + DateTimeOffset.Now.ToString("yyyyMMdd-HHmmss", CultureInfo.InvariantCulture) + ".log");
            Log("BEGIN version=" + plan.InstallerVersion
                + " story=" + plan.Story
                + " profile=" + plan.Profile
                + " channel=" + plan.ReleaseChannel
                + " consent=" + SafeToken(consent.PlanFingerprint));
            return logPath;
        }

        public void InstallOrUpdateWsl(
            InstallationAction action,
            CancellationToken cancellationToken)
        {
            Log("WSL action=" + action.Disposition);
            ProcessResult result = RunElevated(
                "wsl.exe", "--install --no-distribution", TimeSpan.FromMinutes(15), cancellationToken);
            if (result.ExitCode != 0)
            {
                throw new InvalidOperationException(
                    "WSL no pudo instalarse o actualizarse (código " + result.ExitCode
                    + "). Revise UAC, políticas y reinicie si Windows lo solicita.");
            }
        }

        public void InstallDocker(
            InstallerConfiguration configuration,
            InstallationAction action,
            CancellationToken cancellationToken)
        {
            string cache = Path.Combine(installationRoot, "cache");
            Directory.CreateDirectory(cache);
            string installer = Path.Combine(
                cache, "Docker-Desktop-" + configuration.DockerDesktopInstallVersion + ".exe");
            if (!File.Exists(installer)
                || !String.Equals(HashFile(installer), configuration.DockerSha256,
                    StringComparison.OrdinalIgnoreCase))
            {
                Log("DOCKER download version=" + configuration.DockerDesktopInstallVersion
                    + " bytes=" + configuration.DockerDownloadBytes);
                Download(configuration.DockerDownloadUrl, installer,
                    configuration.DockerDownloadBytes, cancellationToken);
            }
            VerifyHash(installer, configuration.DockerSha256, "Docker Desktop");
            Log("DOCKER hash=verified license=accepted mode=per-user");
            ProcessResult result = RunProcess(
                installer,
                "install --user --backend=wsl-2 --no-windows-containers --accept-license",
                installationRoot,
                TimeSpan.FromMinutes(30),
                cancellationToken);
            if (result.ExitCode != 0)
            {
                throw new InvalidOperationException(
                    "Docker Desktop devolvió el código " + result.ExitCode
                    + ". La descarga se conserva verificada para reanudar.");
            }
            StartDockerDesktopIfNeeded();
            WaitForDocker(cancellationToken);
        }

        public void DeployPayload(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            string packageDirectory = configuration.PackageDirectory;
            string payload = Path.Combine(packageDirectory, "payload.zip");
            PackageIntegrity.VerifyDeclaredFile(packageDirectory, "payload.zip");

            string releases = Path.Combine(installationRoot, "releases");
            Directory.CreateDirectory(releases);
            releaseDirectory = SafeChild(
                releases, configuration.InstallerVersion.Replace('+', '-'));
            Directory.CreateDirectory(releaseDirectory);
            Log("PAYLOAD hash=verified release=" + configuration.InstallerVersion);
            PackageIntegrity.SafeExtract(payload, releaseDirectory, cancellationToken);

            string compose = Path.Combine(releaseDirectory, "infra", "compose", "compose.yaml");
            string dockerfile = Path.Combine(releaseDirectory, "infra", "docker", "Dockerfile");
            string migratorDockerfile =
                Path.Combine(releaseDirectory, "infra", "docker", "Dockerfile.migrator");
            if (!File.Exists(compose) || !File.Exists(dockerfile) || !File.Exists(migratorDockerfile))
            {
                throw new InvalidDataException(
                    "El payload no contiene la infraestructura mínima declarada.");
            }
        }

        public void EnsureSecretsAndConfiguration(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            InstallationAction action,
            CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            string secrets = Path.Combine(stateDirectory, "secrets");
            Directory.CreateDirectory(secrets);
            string[] names =
            {
                "postgres-password.txt",
                "keycloak-admin-password.txt",
                "oidc-client-secret.txt",
                "demo-user-password.txt"
            };
            foreach (string name in names)
            {
                cancellationToken.ThrowIfCancellationRequested();
                string destination = Path.Combine(secrets, name);
                if (File.Exists(destination))
                {
                    Log("SECRET reused name=" + name);
                    continue;
                }

                string source = String.IsNullOrWhiteSpace(snapshot.ExistingConfigurationSource)
                    ? null
                    : Path.Combine(
                        snapshot.ExistingConfigurationSource, ".tools", "secrets", name);
                if (!String.IsNullOrWhiteSpace(source) && File.Exists(source))
                {
                    File.Copy(source, destination, false);
                    Log("SECRET adopted name=" + name);
                }
                else
                {
                    if (snapshot.ExistingVolumes)
                    {
                        throw new InvalidOperationException(
                            "Existen volúmenes pero falta el secreto previo '" + name
                            + "'. No se generará otro porque podría dejar los datos inaccesibles.");
                    }
                    WriteRandomSecret(destination);
                    Log("SECRET generated name=" + name);
                }
            }

            string environmentFile = Path.Combine(stateDirectory, "compose.env.local");
            WriteEnvironment(configuration, secrets, environmentFile);
            WriteHelpers(configuration, environmentFile);
        }

        public void EnsureImages(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            EnsureReleaseDirectory();
            if (!ImageMatches(configuration.ApplicationImage, configuration.ApplicationDigest,
                cancellationToken))
            {
                Log("IMAGE build component=application");
                RunRequired(
                    "docker.exe",
                    "build --file infra/docker/Dockerfile"
                    + " --build-arg LOGIXONE_BUILD_MODE=verified"
                    + " --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo"
                    + " --tag " + Quote(configuration.ApplicationImage) + " .",
                    releaseDirectory,
                    TimeSpan.FromMinutes(60),
                    cancellationToken,
                    "build de aplicación");
            }
            if (!ImageMatches(configuration.ApplicationImage, configuration.ApplicationDigest,
                cancellationToken))
            {
                throw new InvalidDataException(
                    "La imagen de aplicación no coincide con el digest congelado.");
            }

            if (!ImageMatches(configuration.MigratorImage, configuration.MigratorDigest,
                cancellationToken))
            {
                Log("IMAGE build component=migrator");
                RunRequired(
                    "docker.exe",
                    "build --file infra/docker/Dockerfile.migrator"
                    + " --build-arg LOGIXONE_BUILD_MODE=verified"
                    + " --build-arg LOGIXONE_MAVEN_PROFILE=with-inventory-demo"
                    + " --tag " + Quote(configuration.MigratorImage) + " .",
                    releaseDirectory,
                    TimeSpan.FromMinutes(60),
                    cancellationToken,
                    "build del migrador");
            }
            if (!ImageMatches(configuration.MigratorImage, configuration.MigratorDigest,
                cancellationToken))
            {
                throw new InvalidDataException(
                    "La imagen del migrador no coincide con el digest congelado.");
            }
            Log("IMAGE digests=verified");
        }

        public void StartCompose(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            EnsureReleaseDirectory();
            string environmentFile = Path.Combine(stateDirectory, "compose.env.local");
            string composeFile = Path.Combine(releaseDirectory, "infra", "compose", "compose.yaml");
            string prefix = "compose --env-file " + Quote(environmentFile)
                + " -f " + Quote(composeFile);
            Log("COMPOSE migrate");
            RunRequired(
                "docker.exe", prefix + " run --rm migrator",
                releaseDirectory, TimeSpan.FromMinutes(10), cancellationToken, "migración");
            Log("COMPOSE up preserve-volumes=true");
            RunRequired(
                "docker.exe", prefix + " up -d --wait --wait-timeout 240",
                releaseDirectory, TimeSpan.FromMinutes(8), cancellationToken, "arranque Compose");
        }

        public void VerifyHealth(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            WaitForHealth(configuration.LivenessUrl, "liveness", cancellationToken);
            WaitForHealth(configuration.ReadinessUrl, "readiness", cancellationToken);
            Log("HEALTH liveness=UP readiness=UP");
        }

        public void Complete(
            InstallerConfiguration configuration,
            InstallationPlan plan,
            InstallationResult result)
        {
            var serializer = new JavaScriptSerializer();
            var state = new Dictionary<string, object>
            {
                { "schemaVersion", 1 },
                { "product", configuration.Product },
                { "installerVersion", configuration.InstallerVersion },
                { "story", configuration.Story },
                { "profile", configuration.Profile },
                { "releaseChannel", configuration.ReleaseChannel },
                { "installedAt", result.FinishedAt.ToString("o", CultureInfo.InvariantCulture) },
                { "releaseDirectory", releaseDirectory },
                { "applicationImage", configuration.ApplicationImage },
                { "applicationDigest", configuration.ApplicationDigest },
                { "migratorImage", configuration.MigratorImage },
                { "migratorDigest", configuration.MigratorDigest },
                { "dataPolicy", configuration.DataPolicy },
                { "composeProject", configuration.ComposeProject }
            };
            string statePath = Path.Combine(installationRoot, "install-state.json");
            WriteUtf8Atomic(statePath, serializer.Serialize(state) + Environment.NewLine);
            WriteUtf8Atomic(
                Path.Combine(installationRoot, "current-release.txt"),
                releaseDirectory + Environment.NewLine);
            Log("COMPLETE state=written dataPolicy=" + configuration.DataPolicy);
        }

        public void Fail(string safeMessage)
        {
            if (!String.IsNullOrWhiteSpace(logPath))
            {
                Log("FAILED " + SafeMessage(safeMessage));
            }
        }

        private void WriteEnvironment(
            InstallerConfiguration configuration,
            string secrets,
            string environmentFile)
        {
            var values = ReadEnvironment(environmentFile);
            SetDefault(values, "COMPOSE_PROJECT_NAME", configuration.ComposeProject);
            SetDefault(values, "LOGIXONE_HTTP_BIND", "127.0.0.1");
            SetDefault(values, "LOGIXONE_HTTP_PORT", "18080");
            SetDefault(values, "LOGIXONE_KEYCLOAK_BIND", "127.0.0.1");
            SetDefault(values, "LOGIXONE_KEYCLOAK_PORT", "8180");
            SetDefault(values, "LOGIXONE_KEYCLOAK_PUBLIC_URL", "http://keycloak.localhost:8180");
            SetDefault(values, "LOGIXONE_KEYCLOAK_PROXY_HEADERS", "xforwarded");
            SetDefault(values, "LOGIXONE_KEYCLOAK_ADMIN_USER", "logixone-admin");
            SetDefault(values, "LOGIXONE_DB_NAME", "logixone");
            SetDefault(values, "LOGIXONE_DB_USER", "logixone");
            SetDefault(values, "LOGIXONE_TX_NODE_ID", "logixone-local-1");
            values["LOGIXONE_POSTGRES_PASSWORD_FILE"] =
                DockerPath(Path.Combine(secrets, "postgres-password.txt"));
            values["LOGIXONE_KEYCLOAK_ADMIN_PASSWORD_FILE"] =
                DockerPath(Path.Combine(secrets, "keycloak-admin-password.txt"));
            values["LOGIXONE_OIDC_CLIENT_SECRET_FILE"] =
                DockerPath(Path.Combine(secrets, "oidc-client-secret.txt"));
            values["LOGIXONE_DEMO_USER_PASSWORD_FILE"] =
                DockerPath(Path.Combine(secrets, "demo-user-password.txt"));
            SetDefault(values, "LOGIXONE_OIDC_PROVIDER_URL",
                "http://keycloak.localhost:8180/realms/logixone");
            SetDefault(values, "LOGIXONE_OIDC_CLIENT_ID", "logixone-web");
            SetDefault(values, "LOGIXONE_OIDC_REDIRECT_URI", "http://localhost:18080/logixone/*");
            SetDefault(values, "LOGIXONE_OIDC_WEB_ORIGIN", "http://localhost:18080");
            SetDefault(values, "LOGIXONE_OIDC_POST_LOGOUT_REDIRECT_URI",
                "http://localhost:18080/logixone/faces/app/index.xhtml");
            SetDefault(values, "LOGIXONE_PROXY_ADDRESS_FORWARDING", "false");
            SetDefault(values, "LOGIXONE_SECURITY_BOOTSTRAP_ENABLED", "false");
            SetDefault(values, "LOGIXONE_DEMO_PROVISIONING_ENABLED", "true");
            SetDefault(values, "LOGIXONE_DEMO_SUBJECT_NO_COMPANY",
                "10000000-0000-4000-8000-000000000001");
            SetDefault(values, "LOGIXONE_DEMO_SUBJECT_SINGLE_COMPANY",
                "10000000-0000-4000-8000-000000000002");
            SetDefault(values, "LOGIXONE_DEMO_SUBJECT_MULTIPLE_COMPANIES",
                "10000000-0000-4000-8000-000000000003");
            values["LOGIXONE_APP_IMAGE"] = configuration.ApplicationImage;
            values["LOGIXONE_MIGRATOR_IMAGE"] = configuration.MigratorImage;

            var lines = values.OrderBy(item => item.Key, StringComparer.Ordinal)
                .Select(item => item.Key + "=" + EnvironmentValue(item.Value));
            WriteUtf8Atomic(environmentFile,
                "# Generado por Logixone Installer; no contiene secretos." + Environment.NewLine
                + String.Join(Environment.NewLine, lines) + Environment.NewLine);
            Log("CONFIG environment=updated secrets=external-files");
        }

        private void WriteHelpers(
            InstallerConfiguration configuration,
            string environmentFile)
        {
            string composeFile = Path.Combine(releaseDirectory, "infra", "compose", "compose.yaml");
            string common = "docker compose --env-file \"" + environmentFile
                + "\" -f \"" + composeFile + "\"";
            WriteUtf8Atomic(
                Path.Combine(installationRoot, "Start-Logixone.cmd"),
                "@echo off" + Environment.NewLine
                + common + " up -d --wait --wait-timeout 240" + Environment.NewLine
                + "if errorlevel 1 pause" + Environment.NewLine);
            WriteUtf8Atomic(
                Path.Combine(installationRoot, "Stop-Logixone.cmd"),
                "@echo off" + Environment.NewLine
                + "rem Los volúmenes de datos deben conservarse." + Environment.NewLine
                + common + " down" + Environment.NewLine
                + "if errorlevel 1 pause" + Environment.NewLine);
            WriteUtf8Atomic(
                Path.Combine(installationRoot, "Abrir-Logixone.url"),
                "[InternetShortcut]" + Environment.NewLine
                + "URL=" + configuration.ApplicationUrl + Environment.NewLine);
        }

        private static IDictionary<string, string> ReadEnvironment(string path)
        {
            var values = new Dictionary<string, string>(StringComparer.Ordinal);
            if (!File.Exists(path))
            {
                return values;
            }
            foreach (string line in File.ReadAllLines(path, Encoding.UTF8))
            {
                string current = line.Trim();
                if (current.Length == 0 || current.StartsWith("#", StringComparison.Ordinal))
                {
                    continue;
                }
                int separator = current.IndexOf('=');
                if (separator <= 0) continue;
                values[current.Substring(0, separator)] =
                    current.Substring(separator + 1).Trim().Trim('"');
            }
            return values;
        }

        private static void SetDefault(
            IDictionary<string, string> values,
            string key,
            string value)
        {
            if (!values.ContainsKey(key))
            {
                values[key] = value;
            }
        }

        private static string EnvironmentValue(string value)
        {
            if (value.IndexOfAny(new[] { ' ', '#', ';' }) >= 0)
            {
                return "\"" + value.Replace("\"", "\\\"") + "\"";
            }
            return value;
        }

        private static string DockerPath(string path)
        {
            return Path.GetFullPath(path).Replace('\\', '/');
        }

        private static void WriteRandomSecret(string path)
        {
            byte[] data = new byte[32];
            using (var generator = new RNGCryptoServiceProvider())
            {
                generator.GetBytes(data);
            }
            File.WriteAllText(path, Convert.ToBase64String(data) + Environment.NewLine,
                new UTF8Encoding(false));
        }

        private bool ImageMatches(
            string image,
            string expectedDigest,
            CancellationToken cancellationToken)
        {
            ProcessResult result = RunProcess(
                "docker.exe",
                "image inspect --format \"{{.Id}}\" " + Quote(image),
                installationRoot,
                TimeSpan.FromSeconds(20),
                cancellationToken);
            return result.ExitCode == 0
                && String.Equals(result.Output.Trim(), expectedDigest, StringComparison.OrdinalIgnoreCase);
        }

        private void WaitForHealth(
            string url,
            string label,
            CancellationToken cancellationToken)
        {
            DateTimeOffset deadline = DateTimeOffset.Now.AddMinutes(4);
            while (DateTimeOffset.Now < deadline)
            {
                cancellationToken.ThrowIfCancellationRequested();
                try
                {
                    var request = (HttpWebRequest)WebRequest.Create(url);
                    request.Method = "GET";
                    request.Timeout = 5000;
                    request.ReadWriteTimeout = 5000;
                    using (var response = (HttpWebResponse)request.GetResponse())
                    using (var reader = new StreamReader(response.GetResponseStream()))
                    {
                        string body = reader.ReadToEnd();
                        if ((int)response.StatusCode == 200
                            && body.IndexOf("\"UP\"", StringComparison.OrdinalIgnoreCase) >= 0)
                        {
                            return;
                        }
                    }
                }
                catch (WebException)
                {
                    // El servicio puede seguir arrancando.
                }
                if (cancellationToken.WaitHandle.WaitOne(TimeSpan.FromSeconds(3)))
                {
                    cancellationToken.ThrowIfCancellationRequested();
                }
            }
            throw new InvalidOperationException(
                "El control " + label + " no llegó a UP dentro de cuatro minutos.");
        }

        private void StartDockerDesktopIfNeeded()
        {
            string[] candidates =
            {
                Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "Programs", "DockerDesktop", "Docker Desktop.exe"),
                Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
                    "Docker", "Docker", "Docker Desktop.exe")
            };
            string executable = candidates.FirstOrDefault(File.Exists);
            if (String.IsNullOrWhiteSpace(executable))
            {
                return;
            }
            Process.Start(new ProcessStartInfo
            {
                FileName = executable,
                UseShellExecute = true
            });
        }

        private void WaitForDocker(CancellationToken cancellationToken)
        {
            DateTimeOffset deadline = DateTimeOffset.Now.AddMinutes(4);
            while (DateTimeOffset.Now < deadline)
            {
                cancellationToken.ThrowIfCancellationRequested();
                ProcessResult result = RunProcess(
                    "docker.exe", "version --format \"{{.Server.Version}}\"",
                    installationRoot, TimeSpan.FromSeconds(10), cancellationToken);
                if (result.ExitCode == 0 && !String.IsNullOrWhiteSpace(result.Output))
                {
                    return;
                }
                if (cancellationToken.WaitHandle.WaitOne(TimeSpan.FromSeconds(3)))
                {
                    cancellationToken.ThrowIfCancellationRequested();
                }
            }
            throw new InvalidOperationException(
                "Docker Desktop se instaló, pero el motor no quedó disponible. "
                + "Ábralo, acepte sus términos si corresponde y reanude la reparación.");
        }

        private static void Download(
            string url,
            string destination,
            long expectedBytes,
            CancellationToken cancellationToken)
        {
            string partial = destination + ".partial";
            var request = (HttpWebRequest)WebRequest.Create(url);
            request.Method = "GET";
            request.AllowAutoRedirect = true;
            request.Timeout = 30000;
            request.ReadWriteTimeout = 30000;
            request.UserAgent = "Logixone-Installer";
            using (var response = (HttpWebResponse)request.GetResponse())
            using (Stream input = response.GetResponseStream())
            using (var output = new FileStream(partial, FileMode.Create, FileAccess.Write, FileShare.None))
            {
                var buffer = new byte[1024 * 1024];
                int read;
                long total = 0;
                while ((read = input.Read(buffer, 0, buffer.Length)) > 0)
                {
                    cancellationToken.ThrowIfCancellationRequested();
                    output.Write(buffer, 0, read);
                    total += read;
                    if (expectedBytes > 0 && total > expectedBytes)
                    {
                        throw new InvalidDataException(
                            "La descarga excedió el tamaño declarado.");
                    }
                }
                if (expectedBytes > 0 && total != expectedBytes)
                {
                    throw new InvalidDataException(
                        "La descarga no coincide con el tamaño declarado.");
                }
            }
            File.Copy(partial, destination, true);
            File.Delete(partial);
        }

        private static void VerifyHash(string path, string expected, string label)
        {
            string actual = HashFile(path);
            if (!String.Equals(actual, expected, StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidDataException(
                    "El SHA-256 de " + label + " no coincide con el manifiesto.");
            }
        }

        internal static string HashFile(string path)
        {
            using (SHA256 sha = SHA256.Create())
            using (FileStream input = File.OpenRead(path))
            {
                return String.Concat(sha.ComputeHash(input)
                    .Select(value => value.ToString("x2", CultureInfo.InvariantCulture)));
            }
        }

        private void RunRequired(
            string executable,
            string arguments,
            string workingDirectory,
            TimeSpan timeout,
            CancellationToken cancellationToken,
            string operation)
        {
            ProcessResult result = RunProcess(
                executable, arguments, workingDirectory, timeout, cancellationToken);
            Log("PROCESS operation=" + operation + " exit=" + result.ExitCode);
            if (result.ExitCode != 0)
            {
                throw new InvalidOperationException(
                    "Falló " + operation + " (código " + result.ExitCode
                    + "). Consulte " + logPath + " y reanude con Reparar.");
            }
        }

        internal static ProcessResult RunProcess(
            string executable,
            string arguments,
            string workingDirectory,
            TimeSpan timeout,
            CancellationToken cancellationToken)
        {
            var output = new StringBuilder();
            var start = new ProcessStartInfo
            {
                FileName = executable,
                Arguments = arguments,
                WorkingDirectory = workingDirectory,
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };
            using (Process process = new Process { StartInfo = start })
            {
                process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs eventArgs)
                {
                    AppendCapped(output, eventArgs.Data);
                };
                process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs eventArgs)
                {
                    AppendCapped(output, eventArgs.Data);
                };
                process.Start();
                process.BeginOutputReadLine();
                process.BeginErrorReadLine();
                DateTimeOffset deadline = DateTimeOffset.Now.Add(timeout);
                while (!process.WaitForExit(250))
                {
                    if (cancellationToken.IsCancellationRequested)
                    {
                        try { process.Kill(); } catch { }
                        cancellationToken.ThrowIfCancellationRequested();
                    }
                    if (DateTimeOffset.Now >= deadline)
                    {
                        try { process.Kill(); } catch { }
                        throw new TimeoutException(
                            "La operación excedió el tiempo permitido.");
                    }
                }
                process.WaitForExit();
                return new ProcessResult(process.ExitCode, output.ToString());
            }
        }

        private static ProcessResult RunElevated(
            string executable,
            string arguments,
            TimeSpan timeout,
            CancellationToken cancellationToken)
        {
            var start = new ProcessStartInfo
            {
                FileName = executable,
                Arguments = arguments,
                UseShellExecute = true,
                Verb = "runas"
            };
            using (Process process = Process.Start(start))
            {
                if (process == null) return new ProcessResult(-1, String.Empty);
                DateTimeOffset deadline = DateTimeOffset.Now.Add(timeout);
                while (!process.WaitForExit(250))
                {
                    if (cancellationToken.IsCancellationRequested)
                    {
                        try { process.Kill(); } catch { }
                        cancellationToken.ThrowIfCancellationRequested();
                    }
                    if (DateTimeOffset.Now >= deadline)
                    {
                        throw new TimeoutException(
                            "La operación elevada excedió el tiempo permitido.");
                    }
                }
                return new ProcessResult(process.ExitCode, String.Empty);
            }
        }

        private static void AppendCapped(StringBuilder output, string value)
        {
            if (String.IsNullOrEmpty(value) || output.Length >= 32000) return;
            int remaining = 32000 - output.Length;
            output.AppendLine(value.Length <= remaining ? value : value.Substring(0, remaining));
        }

        private static string Quote(string value)
        {
            return "\"" + (value ?? String.Empty).Replace("\"", "\\\"") + "\"";
        }

        private void EnsureReleaseDirectory()
        {
            if (String.IsNullOrWhiteSpace(releaseDirectory)
                || !Directory.Exists(releaseDirectory))
            {
                throw new InvalidOperationException(
                    "El payload todavía no fue desplegado.");
            }
        }

        private static string SafeInstallationRoot(string path)
        {
            string full = Path.GetFullPath(path);
            string root = Path.GetPathRoot(full);
            if (!Path.IsPathRooted(path)
                || String.Equals(full.TrimEnd('\\'), root.TrimEnd('\\'),
                    StringComparison.OrdinalIgnoreCase)
                || full.Length >= 180)
            {
                throw new InvalidOperationException("La ruta de instalación no es segura.");
            }
            return full.TrimEnd('\\');
        }

        private static string SafeChild(string parent, string name)
        {
            string root = Path.GetFullPath(parent).TrimEnd('\\') + "\\";
            string child = Path.GetFullPath(Path.Combine(root, name));
            if (!child.StartsWith(root, StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidOperationException(
                    "La ruta derivada sale del directorio de instalación.");
            }
            return child;
        }

        private static string SafeToken(string value)
        {
            if (String.IsNullOrWhiteSpace(value)) return "none";
            return value.Length <= 12 ? value : value.Substring(0, 12);
        }

        private static string SafeMessage(string value)
        {
            string message = (value ?? String.Empty).Replace("\r", " ").Replace("\n", " ");
            return message.Length > 500 ? message.Substring(0, 500) : message;
        }

        private void Log(string message)
        {
            File.AppendAllText(
                logPath,
                DateTimeOffset.Now.ToString("o", CultureInfo.InvariantCulture)
                + " " + SafeMessage(message) + Environment.NewLine,
                new UTF8Encoding(false));
        }

        private static void WriteUtf8Atomic(string path, string content)
        {
            string directory = Path.GetDirectoryName(path);
            Directory.CreateDirectory(directory);
            string temporary = path + ".new";
            File.WriteAllText(temporary, content, new UTF8Encoding(false));
            if (File.Exists(path))
            {
                File.Copy(path, path + ".previous", true);
            }
            File.Copy(temporary, path, true);
            File.Delete(temporary);
        }
    }

    internal sealed class ProcessResult
    {
        public ProcessResult(int exitCode, string output)
        {
            ExitCode = exitCode;
            Output = output ?? String.Empty;
        }

        public int ExitCode { get; private set; }
        public string Output { get; private set; }
    }

    internal static class PackageIntegrity
    {
        public static void VerifyDeclaredFile(string packageDirectory, string fileName)
        {
            string sums = Path.Combine(packageDirectory, "SHA256SUMS.txt");
            if (!File.Exists(sums))
            {
                throw new InvalidDataException("Falta SHA256SUMS.txt.");
            }
            string expected = null;
            foreach (string line in File.ReadAllLines(sums, Encoding.ASCII))
            {
                string current = line.Trim();
                if (current.Length < 67) continue;
                string hash = current.Substring(0, 64);
                string declared = current.Substring(64).Trim().TrimStart('*').Trim();
                if (String.Equals(declared, fileName, StringComparison.Ordinal))
                {
                    expected = hash;
                    break;
                }
            }
            if (String.IsNullOrWhiteSpace(expected))
            {
                throw new InvalidDataException(
                    "El archivo '" + fileName + "' no está declarado en SHA256SUMS.txt.");
            }
            string path = Path.Combine(packageDirectory, fileName);
            if (!File.Exists(path)
                || !String.Equals(WindowsInstallationOperations.HashFile(path), expected,
                    StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidDataException(
                    "El archivo '" + fileName + "' no coincide con su SHA-256.");
            }
        }

        public static void SafeExtract(
            string archivePath,
            string destinationRoot,
            CancellationToken cancellationToken)
        {
            string root = Path.GetFullPath(destinationRoot)
                .TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
            using (ZipArchive archive = ZipFile.OpenRead(archivePath))
            {
                foreach (ZipArchiveEntry entry in archive.Entries)
                {
                    cancellationToken.ThrowIfCancellationRequested();
                    string relative = entry.FullName
                        .Replace('/', Path.DirectorySeparatorChar)
                        .Replace('\\', Path.DirectorySeparatorChar);
                    if (String.IsNullOrWhiteSpace(relative)
                        || Path.IsPathRooted(relative)
                        || relative.IndexOf(':') >= 0)
                    {
                        throw new InvalidDataException(
                            "El payload contiene una ruta no permitida.");
                    }
                    string destination = Path.GetFullPath(Path.Combine(root, relative));
                    if (!destination.StartsWith(root, StringComparison.OrdinalIgnoreCase))
                    {
                        throw new InvalidDataException(
                            "El payload intenta salir del destino.");
                    }
                    if (entry.FullName.EndsWith("/", StringComparison.Ordinal)
                        || entry.FullName.EndsWith("\\", StringComparison.Ordinal))
                    {
                        Directory.CreateDirectory(destination);
                        continue;
                    }
                    Directory.CreateDirectory(Path.GetDirectoryName(destination));
                    entry.ExtractToFile(destination, true);
                }
            }
        }
    }
}
