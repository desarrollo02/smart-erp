using System;
using System.Drawing;
using System.Globalization;
using System.Linq;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace Logixone.Installer
{
    internal sealed class InstallerForm : Form
    {
        private static readonly Color Primary = Color.FromArgb(0, 105, 92);
        private static readonly Color PrimaryDark = Color.FromArgb(0, 77, 64);
        private static readonly Color Surface = Color.FromArgb(247, 250, 249);
        private static readonly Color OnSurface = Color.FromArgb(28, 31, 30);

        private readonly InstallerConfiguration configuration;
        private readonly string installationDirectory;
        private readonly bool probeNetwork;
        private readonly Label statusLabel;
        private readonly Label summaryLabel;
        private readonly DataGridView checksGrid;
        private readonly DataGridView planGrid;
        private readonly TabControl tabs;
        private readonly Button analyzeButton;
        private readonly Button planButton;
        private readonly Button executeButton;
        private readonly Button cancelButton;
        private CheckBox consentCheck;
        private Label planSummary;
        private Label progressLabel;
        private ProgressBar progressBar;
        private CancellationTokenSource cancellation;
        private bool isExecuting;
        private SystemSnapshot snapshot;
        private PreflightReport report;
        private InstallationPlan plan;

        public InstallerForm(
            InstallerConfiguration configuration,
            string installationDirectory,
            bool probeNetwork)
        {
            if (configuration == null) throw new ArgumentNullException("configuration");
            this.configuration = configuration;
            this.installationDirectory = installationDirectory;
            this.probeNetwork = probeNetwork;

            Text = "Instalador de Logixone " + configuration.InstallerVersion;
            StartPosition = FormStartPosition.CenterScreen;
            MinimumSize = new Size(900, 600);
            Rectangle workingArea = Screen.PrimaryScreen.WorkingArea;
            Size = new Size(
                Math.Min(1080, Math.Max(900, workingArea.Width - 40)),
                Math.Min(700, Math.Max(600, workingArea.Height - 30)));
            BackColor = Surface;
            Font = new Font("Segoe UI", 9F, FontStyle.Regular, GraphicsUnit.Point);
            AutoScaleMode = AutoScaleMode.Dpi;

            var header = BuildHeader();
            statusLabel = new Label
            {
                AutoSize = true,
                Font = new Font("Segoe UI Semibold", 10F),
                ForeColor = Color.White,
                BackColor = Color.FromArgb(69, 90, 100),
                Padding = new Padding(12, 6, 12, 6),
                Text = "SIN ANALIZAR",
                Anchor = AnchorStyles.Top | AnchorStyles.Right
            };
            header.Controls.Add(statusLabel);
            header.Resize += delegate { PositionStatusLabel(); };

            summaryLabel = new Label
            {
                Dock = DockStyle.Top,
                Height = 58,
                Padding = new Padding(24, 12, 24, 8),
                ForeColor = OnSurface,
                Text = "El diagnóstico inicial es de solo lectura y no solicita UAC."
            };

            tabs = new TabControl
            {
                Dock = DockStyle.Fill,
                Padding = new Point(18, 8)
            };
            checksGrid = CreateGrid();
            planGrid = CreateGrid();
            tabs.TabPages.Add(BuildDiagnosticPage());
            tabs.TabPages.Add(BuildPlanPage());
            tabs.TabPages[1].Enabled = false;

            var footer = new Panel
            {
                Dock = DockStyle.Bottom,
                Height = 74,
                BackColor = Color.White,
                Padding = new Padding(20, 16, 20, 14)
            };
            analyzeButton = CreateButton("Analizar nuevamente", false);
            analyzeButton.Click += async delegate { await AnalyzeAsync(); };
            planButton = CreateButton("Revisar plan", true);
            planButton.Enabled = false;
            planButton.Click += delegate { ShowPlan(); };
            executeButton = CreateButton("Instalar Logixone", true);
            executeButton.Enabled = false;
            executeButton.Click += delegate { ExecuteAcceptedPlan(); };
            cancelButton = CreateButton("Cancelar", false);
            cancelButton.Click += delegate
            {
                if (isExecuting && cancellation != null)
                {
                    cancellation.Cancel();
                    cancelButton.Enabled = false;
                    progressLabel.Text = "Cancelando al llegar a un límite seguro…";
                }
                else
                {
                    Close();
                }
            };

            var buttons = new FlowLayoutPanel
            {
                Dock = DockStyle.Right,
                AutoSize = true,
                FlowDirection = FlowDirection.LeftToRight,
                WrapContents = false
            };
            buttons.Controls.Add(analyzeButton);
            buttons.Controls.Add(planButton);
            buttons.Controls.Add(executeButton);
            buttons.Controls.Add(cancelButton);
            footer.Controls.Add(buttons);

            Controls.Add(tabs);
            Controls.Add(summaryLabel);
            Controls.Add(footer);
            Controls.Add(header);

            Shown += async delegate { await AnalyzeAsync(); };
        }

        internal static int SmokeTest(InstallerConfiguration configuration)
        {
            using (var form = new InstallerForm(
                configuration, configuration.DefaultInstallationDirectory, false))
            {
                form.CreateControl();
                form.OnLoad(EventArgs.Empty);
                return form.tabs.TabPages.Count == 2
                    && form.checksGrid.Columns.Count == 5
                    && form.planGrid.Columns.Count == 6
                    ? 0 : 1;
            }
        }

        private Panel BuildHeader()
        {
            var header = new Panel
            {
                Dock = DockStyle.Top,
                Height = 94,
                BackColor = PrimaryDark
            };
            var title = new Label
            {
                AutoSize = true,
                Location = new Point(24, 18),
                Font = new Font("Segoe UI Semibold", 20F),
                ForeColor = Color.White,
                Text = "Logixone"
            };
            var subtitle = new Label
            {
                AutoSize = true,
                Location = new Point(27, 57),
                ForeColor = Color.FromArgb(210, 234, 230),
                Text = configuration.Story + " · Sprint " + configuration.Sprint
                    + " · " + configuration.ReleaseChannel
            };
            header.Controls.Add(title);
            header.Controls.Add(subtitle);
            return header;
        }

        private TabPage BuildDiagnosticPage()
        {
            var page = new TabPage("1. Compatibilidad")
            {
                Padding = new Padding(12),
                BackColor = Surface
            };
            var intro = new Label
            {
                Dock = DockStyle.Top,
                Height = 44,
                Text = "Sistema, hardware, WSL, Docker, puertos, permisos, red e instalación previa."
            };
            page.Controls.Add(checksGrid);
            page.Controls.Add(intro);
            return page;
        }

        private TabPage BuildPlanPage()
        {
            var page = new TabPage("2. Plan y consentimiento")
            {
                Padding = new Padding(12),
                BackColor = Surface
            };
            planSummary = new Label
            {
                Dock = DockStyle.Top,
                Height = 72,
                Text = "El plan aparecerá después de un diagnóstico no bloqueado."
            };
            consentCheck = new CheckBox
            {
                Dock = DockStyle.Bottom,
                Height = 54,
                Padding = new Padding(8, 6, 8, 6),
                Text = "He revisado acciones, licencias, descargas, rutas, puertos y reinicios; "
                    + "acepto iniciar los cambios descritos."
            };
            consentCheck.CheckedChanged += delegate
            {
                executeButton.Enabled = consentCheck.Checked
                    && plan != null
                    && File.Exists(Path.Combine(configuration.PackageDirectory, "payload.zip"));
            };
            progressLabel = new Label
            {
                Dock = DockStyle.Bottom,
                Height = 28,
                Padding = new Padding(8, 5, 8, 0),
                Text = "Sin cambios iniciados."
            };
            progressBar = new ProgressBar
            {
                Dock = DockStyle.Bottom,
                Height = 12,
                Minimum = 0,
                Maximum = 100
            };
            page.Controls.Add(planGrid);
            page.Controls.Add(consentCheck);
            page.Controls.Add(progressLabel);
            page.Controls.Add(progressBar);
            page.Controls.Add(planSummary);
            return page;
        }

        private async Task AnalyzeAsync()
        {
            analyzeButton.Enabled = false;
            planButton.Enabled = false;
            executeButton.Enabled = false;
            consentCheck.Checked = false;
            SetStatus("ANALIZANDO", Color.FromArgb(69, 90, 100));
            summaryLabel.Text = "Leyendo requisitos del equipo. No se crearán archivos ni se solicitará UAC.";
            checksGrid.Rows.Clear();
            planGrid.Rows.Clear();
            tabs.SelectedIndex = 0;
            tabs.TabPages[1].Enabled = false;

            try
            {
                snapshot = await Task.Factory.StartNew(delegate
                {
                    return WindowsSystemProbe.Capture(
                        configuration, installationDirectory, probeNetwork);
                });
                report = PreflightEvaluator.Evaluate(configuration, snapshot);
                PopulateChecks(report);
                ApplyStatus(report.Status);
                planButton.Enabled = report.Status != CompatibilityStatus.Blocked;
            }
            catch (Exception exception)
            {
                SetStatus("BLOQUEADA", Color.FromArgb(179, 38, 30));
                summaryLabel.Text = "El diagnóstico falló de forma cerrada: " + exception.Message;
            }
            finally
            {
                analyzeButton.Enabled = true;
            }
        }

        private void PopulateChecks(PreflightReport currentReport)
        {
            foreach (CheckResult check in currentReport.Checks)
            {
                int row = checksGrid.Rows.Add(
                    LevelSymbol(check.Level),
                    check.Title,
                    check.Actual,
                    check.Requirement,
                    check.Level == CheckLevel.Warning || check.Level == CheckLevel.Blocker
                        ? check.Remediation : String.Empty);
                checksGrid.Rows[row].DefaultCellStyle.BackColor = LevelColor(check.Level);
            }
        }

        private void ApplyStatus(CompatibilityStatus status)
        {
            if (status == CompatibilityStatus.Blocked)
            {
                SetStatus(Program.StatusName(status), Color.FromArgb(179, 38, 30));
                summaryLabel.Text = "La máquina está bloqueada. No se realizó ningún cambio ni se solicitará UAC.";
            }
            else if (status == CompatibilityStatus.CompatibleWithWarnings)
            {
                SetStatus(Program.StatusName(status), Color.FromArgb(143, 95, 0));
                summaryLabel.Text = "Puede continuar después de revisar las advertencias. "
                    + "El diagnóstico todavía no realizó cambios.";
            }
            else
            {
                SetStatus(Program.StatusName(status), Primary);
                summaryLabel.Text = "La máquina cumple los requisitos. Revise el plan antes de consentir.";
            }
        }

        private void ShowPlan()
        {
            plan = InstallationPlanBuilder.Build(configuration, snapshot, report);
            planGrid.Rows.Clear();
            foreach (InstallationAction action in plan.Actions)
            {
                planGrid.Rows.Add(
                    action.Order,
                    DispositionText(action.Disposition),
                    action.Component,
                    action.Version,
                    FormatBytes(action.DownloadBytes),
                    action.Description
                        + (String.IsNullOrWhiteSpace(action.LicenseName)
                            ? String.Empty
                            : " Licencia: " + action.LicenseName + " — " + action.LicenseUrl));
            }
            planSummary.Text = "Destino: " + plan.InstallationDirectory
                + Environment.NewLine
                + "Descarga prevista: " + FormatBytes(plan.TotalDownloadBytes)
                + " · UAC: " + (plan.RequiresElevation ? "sí, justo antes de WSL" : "no previsto")
                + " · Reinicio: " + (plan.MayRequireRestart ? "posible" : "no previsto")
                + " · Puertos: " + String.Join(", ", plan.Ports);
            if (!File.Exists(Path.Combine(configuration.PackageDirectory, "payload.zip")))
            {
                planSummary.Text += Environment.NewLine
                    + "El payload todavía no está empaquetado; esta compilación de desarrollo "
                    + "solo permite revisar el plan.";
                consentCheck.Enabled = false;
            }
            else
            {
                consentCheck.Enabled = true;
            }
            tabs.TabPages[1].Enabled = true;
            tabs.SelectedIndex = 1;
        }

        private async void ExecuteAcceptedPlan()
        {
            if (plan == null || !consentCheck.Checked || isExecuting)
            {
                return;
            }
            var consent = new InstallationConsent
            {
                Accepted = true,
                ThirdPartyLicensesAccepted = true,
                AcceptedAt = DateTimeOffset.Now,
                PlanFingerprint = PlanFingerprint.Calculate(plan)
            };
            cancellation = new CancellationTokenSource();
            isExecuting = true;
            executeButton.Enabled = false;
            analyzeButton.Enabled = false;
            planButton.Enabled = false;
            consentCheck.Enabled = false;
            cancelButton.Text = "Cancelar de forma segura";
            progressBar.Value = 0;

            var operations = new WindowsInstallationOperations(snapshot);
            var executor = new InstallationExecutor(operations, delegate(ExecutionProgress current)
            {
                if (IsDisposed) return;
                BeginInvoke((MethodInvoker)delegate
                {
                    progressBar.Maximum = Math.Max(1, current.Total);
                    progressBar.Value = Math.Min(progressBar.Maximum, Math.Max(0, current.Current));
                    progressLabel.Text = current.Current + "/" + current.Total
                        + " · " + current.Message;
                });
            });

            InstallationResult result = await Task.Factory.StartNew(delegate
            {
                return executor.Execute(
                    configuration, snapshot, report, plan, consent, cancellation.Token);
            });

            isExecuting = false;
            cancelButton.Text = "Cerrar";
            cancelButton.Enabled = true;
            progressLabel.Text = result.Outcome == ExecutionOutcome.Succeeded
                ? "Instalación completada y verificada."
                : result.Error;
            if (result.Outcome == ExecutionOutcome.Succeeded)
            {
                SetStatus("INSTALADA", Primary);
                summaryLabel.Text = "Logixone quedó listo. Datos y volúmenes existentes fueron preservados.";
                MessageBox.Show(
                    this,
                    "Instalación verificada." + Environment.NewLine
                        + configuration.ApplicationUrl + Environment.NewLine
                        + "Log: " + result.LogPath,
                    "Logixone listo",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information);
            }
            else
            {
                SetStatus(
                    result.Outcome == ExecutionOutcome.Cancelled ? "CANCELADA" : "FALLÓ",
                    Color.FromArgb(179, 38, 30));
                MessageBox.Show(
                    this,
                    result.Error + Environment.NewLine + "Log: " + result.LogPath,
                    "La instalación no se completó",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Warning);
            }
        }

        private static DataGridView CreateGrid()
        {
            var grid = new DataGridView
            {
                Dock = DockStyle.Fill,
                BackgroundColor = Color.White,
                BorderStyle = BorderStyle.FixedSingle,
                AllowUserToAddRows = false,
                AllowUserToDeleteRows = false,
                AllowUserToResizeRows = false,
                ReadOnly = true,
                RowHeadersVisible = false,
                AutoSizeRowsMode = DataGridViewAutoSizeRowsMode.AllCells,
                SelectionMode = DataGridViewSelectionMode.FullRowSelect,
                MultiSelect = false
            };
            grid.DefaultCellStyle.WrapMode = DataGridViewTriState.True;
            grid.ColumnHeadersDefaultCellStyle.BackColor = PrimaryDark;
            grid.ColumnHeadersDefaultCellStyle.ForeColor = Color.White;
            grid.EnableHeadersVisualStyles = false;
            return grid;
        }

        protected override void OnLoad(EventArgs e)
        {
            base.OnLoad(e);
            checksGrid.Columns.Clear();
            checksGrid.Columns.Add("state", "Estado");
            checksGrid.Columns.Add("check", "Comprobación");
            checksGrid.Columns.Add("actual", "Resultado");
            checksGrid.Columns.Add("requirement", "Requisito");
            checksGrid.Columns.Add("recovery", "Recuperación");
            checksGrid.Columns[0].Width = 72;
            checksGrid.Columns[1].Width = 150;
            checksGrid.Columns[2].Width = 190;
            checksGrid.Columns[3].Width = 250;
            checksGrid.Columns[4].AutoSizeMode = DataGridViewAutoSizeColumnMode.Fill;

            planGrid.Columns.Clear();
            planGrid.Columns.Add("order", "#");
            planGrid.Columns.Add("action", "Acción");
            planGrid.Columns.Add("component", "Componente");
            planGrid.Columns.Add("version", "Versión");
            planGrid.Columns.Add("download", "Descarga");
            planGrid.Columns.Add("description", "Qué hará");
            planGrid.Columns[0].Width = 42;
            planGrid.Columns[1].Width = 92;
            planGrid.Columns[2].Width = 150;
            planGrid.Columns[3].Width = 120;
            planGrid.Columns[4].Width = 94;
            planGrid.Columns[5].AutoSizeMode = DataGridViewAutoSizeColumnMode.Fill;
        }

        private static Button CreateButton(string text, bool primary)
        {
            return new Button
            {
                AutoSize = true,
                Height = 38,
                Margin = new Padding(8, 0, 0, 0),
                Padding = new Padding(14, 0, 14, 0),
                FlatStyle = FlatStyle.Flat,
                BackColor = primary ? Primary : Color.White,
                ForeColor = primary ? Color.White : PrimaryDark,
                Text = text
            };
        }

        private void SetStatus(string text, Color color)
        {
            statusLabel.Text = text;
            statusLabel.BackColor = color;
            PositionStatusLabel();
        }

        private void PositionStatusLabel()
        {
            Control header = statusLabel.Parent;
            if (header == null) return;
            statusLabel.Location = new Point(
                Math.Max(24, header.ClientSize.Width - statusLabel.Width - 24), 28);
        }

        private static string LevelSymbol(CheckLevel level)
        {
            switch (level)
            {
                case CheckLevel.Pass: return "OK";
                case CheckLevel.Information: return "INFO";
                case CheckLevel.Warning: return "AVISO";
                default: return "BLOQUEO";
            }
        }

        private static string DispositionText(PlannedDisposition disposition)
        {
            switch (disposition)
            {
                case PlannedDisposition.Reuse: return "REUTILIZAR";
                case PlannedDisposition.Install: return "INSTALAR";
                case PlannedDisposition.Update: return "ACTUALIZAR";
                case PlannedDisposition.Execute: return "EJECUTAR";
                default: return "VERIFICAR";
            }
        }

        private static Color LevelColor(CheckLevel level)
        {
            switch (level)
            {
                case CheckLevel.Pass: return Color.FromArgb(232, 245, 233);
                case CheckLevel.Information: return Color.FromArgb(227, 242, 253);
                case CheckLevel.Warning: return Color.FromArgb(255, 248, 225);
                default: return Color.FromArgb(255, 235, 238);
            }
        }

        private static string FormatBytes(long bytes)
        {
            if (bytes <= 0) return "0 B";
            double value = bytes;
            string[] units = { "B", "KiB", "MiB", "GiB" };
            int unit = 0;
            while (value >= 1024 && unit < units.Length - 1)
            {
                value /= 1024;
                unit++;
            }
            return value.ToString("0.0", CultureInfo.CurrentCulture) + " " + units[unit];
        }
    }
}
