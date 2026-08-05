using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Text;
using System.Threading;
using Logixone.Installer;

internal static class PreflightEvaluatorTests
{
    private const long GiB = 1024L * 1024L * 1024L;
    private static int assertions;

    private static int Main()
    {
        CompatibleMachineIsAccepted();
        RecommendedShortfallWarns();
        UnsupportedWindowsBlocks();
        ForeignPortBlocks();
        MissingDockerCanBePlanned();
        ExistingLogixonePortsAreReusable();
        ExistingInstallationUsesRepairDiskMinimum();
        OrphanVolumesBlockUntilConfigurationIsSelected();
        PlanExplainsFreshInstall();
        PlanReusesCompatibleRuntime();
        BlockedMachineHasNoPlan();
        ExecutionRequiresConsentBeforeWrites();
        ExecutionRejectsChangedPlanBeforeWrites();
        ExecutionRunsSevenOrderedPhases();
        ExecutionFailureDoesNotInvokeDestructiveRollback();
        CancellationBeforeExecutionHasNoWrites();
        WslElevationRejectionStopsSafely();
        PackageIntegrityAcceptsDeclaredPayload();
        PackageIntegrityRejectsWrongHash();
        PackageExtractionRejectsTraversal();
        InvalidPathBlocks();
        Console.WriteLine("PREFLIGHT_TESTS_OK assertions=" + assertions);
        return 0;
    }

    private static void CompatibleMachineIsAccepted()
    {
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), CompatibleSnapshot());
        Equal(CompatibilityStatus.Compatible, report.Status, "compatible status");
        Equal(16, report.Checks.Count, "complete check matrix");
    }

    private static void RecommendedShortfallWarns()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.TotalMemoryBytes = 12 * GiB;
        snapshot.FreeDiskBytes = 45 * GiB;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.CompatibleWithWarnings, report.Status, "recommended warning status");
        True(HasLevel(report, "memory", CheckLevel.Warning), "memory warning");
        True(HasLevel(report, "disk", CheckLevel.Warning), "disk warning");
    }

    private static void UnsupportedWindowsBlocks()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.WindowsBuild = 22631;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.Blocked, report.Status, "unsupported Windows blocks");
        True(HasLevel(report, "windows", CheckLevel.Blocker), "Windows blocker");
    }

    private static void ForeignPortBlocks()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.PortOwnership[18080] = PortOwnership.Other;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.Blocked, report.Status, "foreign port blocks");
        True(HasLevel(report, "port-18080", CheckLevel.Blocker), "port blocker");
    }

    private static void MissingDockerCanBePlanned()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.DockerCliAvailable = false;
        snapshot.DockerDaemonAvailable = false;
        snapshot.DockerEngineVersion = null;
        snapshot.ComposeVersion = null;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.CompatibleWithWarnings, report.Status, "Docker install is a warning");
        True(HasLevel(report, "docker", CheckLevel.Warning), "Docker warning");
        True(HasLevel(report, "compose", CheckLevel.Warning), "Compose warning");
    }

    private static void ExistingLogixonePortsAreReusable()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.PortOwnership[18080] = PortOwnership.Logixone;
        snapshot.PortOwnership[8180] = PortOwnership.Logixone;
        snapshot.PreviousInstallation = true;
        snapshot.ExistingVolumes = true;
        snapshot.ExistingConfigurationSource = @"C:\existing";
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.Compatible, report.Status, "own ports are compatible");
        True(HasLevel(report, "port-18080", CheckLevel.Information), "own app port");
        True(HasLevel(report, "previous-installation", CheckLevel.Information), "upgrade detected");
    }

    private static void InvalidPathBlocks()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.InstallationDirectory = "relative";
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.Blocked, report.Status, "relative path blocks");
        True(HasLevel(report, "path", CheckLevel.Blocker), "path blocker");
    }

    private static void ExistingInstallationUsesRepairDiskMinimum()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.FreeDiskBytes = 12 * GiB;
        snapshot.ExistingVolumes = true;
        snapshot.ExistingConfigurationSource = @"C:\existing";
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.CompatibleWithWarnings, report.Status, "repair disk warning");
        True(HasLevel(report, "disk", CheckLevel.Warning), "repair disk does not block");
    }

    private static void OrphanVolumesBlockUntilConfigurationIsSelected()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.ExistingVolumes = true;
        snapshot.ExistingConfigurationSource = null;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        Equal(CompatibilityStatus.Blocked, report.Status, "orphan volumes block");
        True(HasLevel(report, "previous-installation", CheckLevel.Blocker),
            "orphan volume blocker");
    }

    private static void PlanExplainsFreshInstall()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.WslAvailable = false;
        snapshot.WslVersion = null;
        snapshot.DockerCliAvailable = false;
        snapshot.DockerDaemonAvailable = false;
        snapshot.DockerEngineVersion = null;
        snapshot.ComposeVersion = null;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        Equal(7, plan.Actions.Count, "plan action count");
        True(plan.RequiresElevation, "WSL elevation declared");
        True(plan.MayRequireRestart, "WSL restart declared");
        Equal(643194800L, plan.TotalDownloadBytes, "Docker download bytes");
    }

    private static void PlanReusesCompatibleRuntime()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        Equal(0L, plan.TotalDownloadBytes, "reuse has no prerequisite download");
        Equal(PlannedDisposition.Reuse, plan.Actions[0].Disposition, "reuse WSL");
        Equal(PlannedDisposition.Reuse, plan.Actions[1].Disposition, "reuse Docker");
        True(!plan.RequiresElevation, "reuse does not elevate");
    }

    private static void BlockedMachineHasNoPlan()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.WindowsBuild = 10000;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        bool rejected = false;
        try
        {
            InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        }
        catch (InvalidOperationException)
        {
            rejected = true;
        }
        True(rejected, "blocked preflight rejects plan");
    }

    private static void ExecutionRequiresConsentBeforeWrites()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations();
        bool rejected = false;
        try
        {
            new InstallationExecutor(operations, null).Execute(
                Configuration(), snapshot, report, plan,
                new InstallationConsent { Accepted = false }, CancellationToken.None);
        }
        catch (InvalidOperationException)
        {
            rejected = true;
        }
        True(rejected, "missing consent rejected");
        Equal(0, operations.Calls.Count, "no write before consent");
    }

    private static void ExecutionRejectsChangedPlanBeforeWrites()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations();
        var consent = Consent(plan);
        plan.Actions[0].Version = "changed";
        bool rejected = false;
        try
        {
            new InstallationExecutor(operations, null).Execute(
                Configuration(), snapshot, report, plan, consent, CancellationToken.None);
        }
        catch (InvalidOperationException)
        {
            rejected = true;
        }
        True(rejected, "changed plan rejected");
        Equal(0, operations.Calls.Count, "changed plan has no write");
    }

    private static void ExecutionRunsSevenOrderedPhases()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations();
        InstallationResult result = new InstallationExecutor(operations, null).Execute(
            Configuration(), snapshot, report, plan, Consent(plan), CancellationToken.None);
        Equal(ExecutionOutcome.Succeeded, result.Outcome, "execution succeeds");
        Equal("begin", operations.Calls[0], "begin is first write");
        Equal("complete", operations.Calls[operations.Calls.Count - 1], "complete last");
        True(operations.Calls.Contains("payload"), "payload executed");
        True(operations.Calls.Contains("secrets"), "secrets executed");
        True(operations.Calls.Contains("images"), "images executed");
        True(operations.Calls.Contains("compose"), "compose executed");
        True(operations.Calls.Contains("health"), "health executed");
    }

    private static void ExecutionFailureDoesNotInvokeDestructiveRollback()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations { FailAt = "compose" };
        InstallationResult result = new InstallationExecutor(operations, null).Execute(
            Configuration(), snapshot, report, plan, Consent(plan), CancellationToken.None);
        Equal(ExecutionOutcome.Failed, result.Outcome, "execution failure reported");
        True(operations.Calls.Contains("fail"), "failure logged");
        True(!operations.Calls.Contains("delete-volumes"), "no destructive rollback");
    }

    private static void CancellationBeforeExecutionHasNoWrites()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations();
        var cancellation = new CancellationTokenSource();
        cancellation.Cancel();
        bool cancelled = false;
        try
        {
            new InstallationExecutor(operations, null).Execute(
                Configuration(), snapshot, report, plan, Consent(plan), cancellation.Token);
        }
        catch (OperationCanceledException)
        {
            cancelled = true;
        }
        True(cancelled, "pre-start cancellation reported");
        Equal(0, operations.Calls.Count, "pre-start cancellation has no writes");
    }

    private static void WslElevationRejectionStopsSafely()
    {
        SystemSnapshot snapshot = CompatibleSnapshot();
        snapshot.WslAvailable = false;
        snapshot.WslVersion = null;
        PreflightReport report = PreflightEvaluator.Evaluate(Configuration(), snapshot);
        InstallationPlan plan = InstallationPlanBuilder.Build(Configuration(), snapshot, report);
        var operations = new RecordingOperations { FailAt = "wsl" };
        InstallationResult result = new InstallationExecutor(operations, null).Execute(
            Configuration(), snapshot, report, plan, Consent(plan), CancellationToken.None);
        Equal(ExecutionOutcome.Failed, result.Outcome, "UAC rejection fails safely");
        True(operations.Calls.Contains("fail"), "UAC rejection logged");
        True(!operations.Calls.Contains("payload"), "UAC rejection stops before payload");
    }

    private static InstallationConsent Consent(InstallationPlan plan)
    {
        return new InstallationConsent
        {
            Accepted = true,
            ThirdPartyLicensesAccepted = true,
            AcceptedAt = DateTimeOffset.Now,
            PlanFingerprint = PlanFingerprint.Calculate(plan)
        };
    }

    private static void PackageIntegrityAcceptsDeclaredPayload()
    {
        string root = TestDirectory("integrity");
        string payload = Path.Combine(root, "payload.zip");
        using (ZipArchive archive = ZipFile.Open(payload, ZipArchiveMode.Create))
        {
            ZipArchiveEntry entry = archive.CreateEntry("infra/compose/compose.yaml");
            using (var writer = new StreamWriter(entry.Open(), new UTF8Encoding(false)))
            {
                writer.Write("name: logixone");
            }
        }
        string hash = WindowsInstallationOperations.HashFile(payload);
        File.WriteAllText(
            Path.Combine(root, "SHA256SUMS.txt"),
            hash + "  payload.zip" + Environment.NewLine,
            Encoding.ASCII);
        PackageIntegrity.VerifyDeclaredFile(root, "payload.zip");
        string destination = Path.Combine(root, "out");
        Directory.CreateDirectory(destination);
        PackageIntegrity.SafeExtract(payload, destination, CancellationToken.None);
        True(File.Exists(Path.Combine(destination, "infra", "compose", "compose.yaml")),
            "safe payload extracted");
        DeleteTestDirectory(root);
    }

    private static void PackageExtractionRejectsTraversal()
    {
        string root = TestDirectory("traversal");
        string payload = Path.Combine(root, "payload.zip");
        using (ZipArchive archive = ZipFile.Open(payload, ZipArchiveMode.Create))
        {
            ZipArchiveEntry entry = archive.CreateEntry("../escape.txt");
            using (var writer = new StreamWriter(entry.Open(), new UTF8Encoding(false)))
            {
                writer.Write("blocked");
            }
        }
        string destination = Path.Combine(root, "out");
        Directory.CreateDirectory(destination);
        bool rejected = false;
        try
        {
            PackageIntegrity.SafeExtract(payload, destination, CancellationToken.None);
        }
        catch (InvalidDataException)
        {
            rejected = true;
        }
        True(rejected, "zip traversal rejected");
        True(!File.Exists(Path.Combine(root, "escape.txt")), "zip traversal wrote nothing outside");
        DeleteTestDirectory(root);
    }

    private static void PackageIntegrityRejectsWrongHash()
    {
        string root = TestDirectory("wrong-hash");
        string payload = Path.Combine(root, "payload.zip");
        File.WriteAllText(payload, "payload", Encoding.ASCII);
        File.WriteAllText(
            Path.Combine(root, "SHA256SUMS.txt"),
            new string('0', 64) + "  payload.zip" + Environment.NewLine,
            Encoding.ASCII);
        bool rejected = false;
        try
        {
            PackageIntegrity.VerifyDeclaredFile(root, "payload.zip");
        }
        catch (InvalidDataException)
        {
            rejected = true;
        }
        True(rejected, "wrong package hash rejected");
        DeleteTestDirectory(root);
    }

    private static string TestDirectory(string label)
    {
        string bin = Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory);
        string root = Path.Combine(bin, "test-temp-" + label + "-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(root);
        return root;
    }

    private static void DeleteTestDirectory(string path)
    {
        string bin = Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory)
            .TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        string target = Path.GetFullPath(path);
        if (!target.StartsWith(bin, StringComparison.OrdinalIgnoreCase)
            || Path.GetFileName(target).IndexOf("test-temp-", StringComparison.Ordinal) != 0)
        {
            throw new InvalidOperationException("unsafe test cleanup target");
        }
        Directory.Delete(target, true);
    }

    private static InstallerConfiguration Configuration()
    {
        return new InstallerConfiguration
        {
            Product = "Logixone Jakarta 11",
            InstallerVersion = "0.8.0-internal.1",
            Sprint = "8",
            Story = "J11-S8-08",
            Profile = "demo-local",
            ReleaseChannel = "INTERNAL_UNSIGNED",
            ApplicationImage = "logixone/app:j11-s8-07-closing",
            ApplicationDigest = "sha256:a44293d0bc1a0df01e4e13025a6bc202266dec82fa6bb5f74f858cd70667d4fb",
            MigratorImage = "logixone/migrator:j11-s8-07-closing",
            MigratorDigest = "sha256:bcf5a51b535c30cb466a10d782f6059bc383ea8db8360575f01a52086451fd81",
            MinimumWindowsBuild = 26100,
            MinimumMemoryBytes = 8 * GiB,
            RecommendedMemoryBytes = 16 * GiB,
            MinimumDiskBytes = 30 * GiB,
            MinimumUpdateDiskBytes = 5 * GiB,
            RecommendedDiskBytes = 60 * GiB,
            MinimumWslVersion = new Version(2, 1, 5),
            MinimumDockerEngineVersion = new Version(29, 6, 0),
            MinimumComposeVersion = new Version(5, 3, 0),
            DockerDesktopInstallVersion = "4.84.0",
            DockerDownloadUrl = "https://desktop.docker.com/example.exe",
            DockerDownloadBytes = 643194800L,
            DockerSha256 = new string('a', 64),
            DockerLicenseName = "Docker Subscription Service Agreement",
            DockerLicenseUrl = "https://www.docker.com/legal/",
            Ports = new List<int> { 18080, 8180 },
            DefaultInstallationDirectory = @"C:\Users\demo\AppData\Local\Logixone\demo-local",
            ComposeProject = "logixone",
            LivenessUrl = "http://localhost:18080/logixone/health/live",
            ReadinessUrl = "http://localhost:18080/logixone/health/ready",
            ApplicationUrl = "http://localhost:18080/logixone/faces/app/index.xhtml",
            DataPolicy = "PRESERVE_VOLUMES",
            GeneratedFiles = new List<string> { "setup.exe" },
            PackageDirectory = @"C:\package"
        };
    }

    private static SystemSnapshot CompatibleSnapshot()
    {
        return new SystemSnapshot
        {
            IsWindows = true,
            Is64BitOperatingSystem = true,
            WindowsProductName = "Windows 11 Pro",
            WindowsDisplayVersion = "25H2",
            WindowsBuild = 26200,
            TotalMemoryBytes = 32 * GiB,
            FreeDiskBytes = 100 * GiB,
            VirtualizationKnown = true,
            VirtualizationEnabled = true,
            SlatKnown = true,
            SlatEnabled = true,
            WslAvailable = true,
            WslVersion = new Version(2, 6, 3),
            DockerCliAvailable = true,
            DockerDaemonAvailable = true,
            DockerEngineVersion = new Version(29, 6, 2),
            ComposeVersion = new Version(5, 3, 1),
            PendingReboot = false,
            NetworkProbed = true,
            NetworkAvailable = true,
            IsAdministrator = false,
            PreviousInstallation = false,
            ExistingVolumes = false,
            ExistingConfigurationSource = null,
            InstallationDirectory = @"C:\Users\demo\AppData\Local\Logixone\demo-local",
            PortOwnership = new Dictionary<int, PortOwnership>
            {
                { 18080, PortOwnership.Free },
                { 8180, PortOwnership.Free }
            }
        };
    }

    private static bool HasLevel(PreflightReport report, string code, CheckLevel level)
    {
        foreach (CheckResult check in report.Checks)
        {
            if (check.Code == code && check.Level == level)
            {
                return true;
            }
        }
        return false;
    }

    private static void Equal<T>(T expected, T actual, string label)
    {
        assertions++;
        if (!EqualityComparer<T>.Default.Equals(expected, actual))
        {
            throw new InvalidOperationException(
                label + ": expected " + expected + ", actual " + actual);
        }
    }

    private static void True(bool value, string label)
    {
        assertions++;
        if (!value)
        {
            throw new InvalidOperationException(label + ": expected true");
        }
    }

    private sealed class RecordingOperations : IInstallationOperations
    {
        public readonly List<string> Calls = new List<string>();
        public string FailAt { get; set; }

        public string Begin(InstallationPlan plan, InstallationConsent consent)
        {
            Record("begin");
            return "test.log";
        }

        public void InstallOrUpdateWsl(InstallationAction action, CancellationToken cancellationToken)
        {
            Record("wsl");
        }

        public void InstallDocker(InstallerConfiguration configuration, InstallationAction action,
            CancellationToken cancellationToken)
        {
            Record("docker");
        }

        public void DeployPayload(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            Record("payload");
        }

        public void EnsureSecretsAndConfiguration(InstallerConfiguration configuration,
            InstallationPlan plan, InstallationAction action, CancellationToken cancellationToken)
        {
            Record("secrets");
        }

        public void EnsureImages(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            Record("images");
        }

        public void StartCompose(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            Record("compose");
        }

        public void VerifyHealth(InstallerConfiguration configuration, InstallationPlan plan,
            CancellationToken cancellationToken)
        {
            Record("health");
        }

        public void Complete(InstallerConfiguration configuration, InstallationPlan plan,
            InstallationResult result)
        {
            Record("complete");
        }

        public void Fail(string safeMessage)
        {
            Calls.Add("fail");
        }

        private void Record(string value)
        {
            Calls.Add(value);
            if (String.Equals(FailAt, value, StringComparison.Ordinal))
            {
                throw new InvalidOperationException("synthetic failure");
            }
        }
    }
}
