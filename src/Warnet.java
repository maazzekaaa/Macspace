import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// ==========================================
// KELAS DASAR (MODUL PRAKTIKUM & DATABASE)
// ==========================================
class ValidasiException extends Exception { public ValidasiException(String pesan) { super(pesan); } }

interface TransaksiItem { double hitungTotal(); }

abstract class LayananDasar implements TransaksiItem {
    protected String namaPelanggan;
    public LayananDasar(String namaPelanggan) { this.namaPelanggan = namaPelanggan; }
    public abstract String getDetailLayanan();
}

class BookingPC extends LayananDasar {
    private String pcType; private int durationMinutes; private double price; private int pcId; private boolean isPlayNow;
    public BookingPC(String namaPelanggan, String pcType, int durationMinutes, double price, int pcId, boolean isPlayNow) {
        super(namaPelanggan); this.pcType = pcType; this.durationMinutes = durationMinutes; this.price = price; this.pcId = pcId; this.isPlayNow = isPlayNow;
    }
    public String getPcType() { return pcType; } public int getDurationMinutes() { return durationMinutes; }
    public double getPrice() { return price; } public int getPcId() { return pcId; } public boolean getIsPlayNow() { return isPlayNow; }
    @Override public double hitungTotal() { return price * (durationMinutes / 60.0); }
    public double hitungTotal(double diskonPersen) { double totalAwal = hitungTotal(); return totalAwal - (totalAwal * (diskonPersen / 100)); }
    @Override public String getDetailLayanan() { return "Booking PC " + pcType + " selama " + durationMinutes + " menit."; }
}

class MenuItem {
    public int id; public String name, type; public double price;
    public MenuItem(int id, String name, String type, double price) { this.id = id; this.name = name; this.type = type; this.price = price; }
}

class OrderItem implements TransaksiItem {
    public MenuItem item; public int quantity;
    public OrderItem(MenuItem item, int quantity) { this.item = item; this.quantity = quantity; }
    @Override public double hitungTotal() { return item.price * quantity; }
}

class ActivePCItem {
    public int transId, pcId; public String pcType;
    public ActivePCItem(int t, int p, String type) { transId = t; pcId = p; pcType = type; }
    @Override public String toString() { return "PC " + pcId + " (" + pcType + ")"; }
}

class Database {
    public static Connection connect() {
        try { return DriverManager.getConnection("jdbc:mysql://localhost:3306/db_warnet", "root", ""); }
        catch (SQLException e) { System.err.println("Gagal koneksi: " + e.getMessage()); return null; }
    }

    public static void setupTables() {
        Connection conn = null; Statement stmt = null;
        try {
            conn = connect(); if (conn == null) return;
            stmt = conn.createStatement();
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(50), role VARCHAR(20))");
            try { stmt.execute("ALTER TABLE users ADD COLUMN saved_time_reguler INT DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN saved_time_vip INT DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN saved_time_vvip INT DEFAULT 0"); } catch (Exception ignore) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS menu (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50), type VARCHAR(20), price DOUBLE)");
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (id INT AUTO_INCREMENT PRIMARY KEY, customer_name VARCHAR(50), pc_type VARCHAR(20), duration INT, duration_minutes INT DEFAULT 0, total_amount DOUBLE, pc_id INT DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, activation_code VARCHAR(20), status VARCHAR(20) DEFAULT 'BELUM_DIPAKAI', activated_at TIMESTAMP NULL, payment_method VARCHAR(20) DEFAULT 'Tunai', booking_datetime DATETIME DEFAULT NULL, is_activated INT DEFAULT 0)");
            try { stmt.execute("ALTER TABLE transactions ADD COLUMN duration_minutes INT DEFAULT 0"); } catch (Exception ignore) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS orders (id INT AUTO_INCREMENT PRIMARY KEY, transaction_id INT, customer_name VARCHAR(50), item_name VARCHAR(50), qty INT, subtotal DOUBLE, status VARCHAR(20) DEFAULT 'PENDING')");
            try { stmt.execute("ALTER TABLE orders ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING'"); } catch (SQLException ignore) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS pcs (id INT PRIMARY KEY, status VARCHAR(20), type VARCHAR(20), floor INT)");

            ResultSet rsAdmin = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'");
            if(rsAdmin.next() && rsAdmin.getInt(1) == 0) stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin', 'ADMIN')");
            
            ResultSet rsUser = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role='USER' AND username='user'");
            if(rsUser.next() && rsUser.getInt(1) == 0) stmt.execute("INSERT INTO users (username, password, role) VALUES ('user', 'user', 'USER')");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pcs");
            if(rs.next() && rs.getInt(1) == 0) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO pcs (id, status, type, floor) VALUES (?, 'TERSEDIA', ?, ?)");
                for(int i = 1; i <= 30; i++) {
                    ps.setInt(1, i);
                    if (i <= 10) { ps.setString(2, "Reguler"); ps.setInt(3, 1); }
                    else if (i <= 20) { ps.setString(2, "VIP"); ps.setInt(3, 2); }
                    else { ps.setString(2, "VVIP"); ps.setInt(3, 3); }
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        finally { try { if (stmt != null) stmt.close(); if (conn != null) conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } }
    }
}

// ==========================================
// TEMA & WARNA MACOS
// ==========================================
class Theme {
    public static boolean isDark = true;
    public static Color bg() { return isDark ? Color.decode("#121212") : Color.decode("#F5F5F7"); }
    public static Color cardBg() { return isDark ? Color.decode("#1E1E1E") : Color.decode("#FFFFFF"); }
    public static Color text() { return isDark ? Color.decode("#FFFFFF") : Color.decode("#1C1C1E"); }
    public static Color textMuted() { return isDark ? Color.decode("#8E8E93") : Color.decode("#8E8E93"); }
    public static Color border() { return isDark ? Color.decode("#38383A") : Color.decode("#D1D1D6"); }
    public static Color accent() { return Color.decode("#0A84FF"); }
    public static Color inputBg() { return isDark ? Color.decode("#2C2C2E") : Color.decode("#E5E5EA"); }
    public static Color btnPrimary() { return Color.decode("#0A84FF"); }
    public static Color btnPrimaryHover() { return Color.decode("#007AFF"); }
    public static Color btnSecondary() { return isDark ? Color.decode("#2C2C2E") : Color.decode("#E5E5EA"); }
    public static Color btnSecondaryHover() { return isDark ? Color.decode("#3A3A3C") : Color.decode("#D1D1D6"); }
    public static Color btnDanger() { return Color.decode("#FF453A"); }
    public static Color warningBg() { return isDark ? new Color(255, 69, 58, 40) : new Color(255, 69, 58, 60); }
    public static Color pcAvailable() { return isDark ? Color.decode("#2C2C2E") : Color.decode("#F2F2F7"); }
    public static Color pcActive() { return Color.decode("#30D158"); }
    public static Color pcMaintenance() { return Color.decode("#FF9F0A"); }
    public static Color pcBooked() { return Color.decode("#BF5AF2"); }

    public static Font getFont(int style, int size) {
        String[] fonts = {"SF Pro Display", "-apple-system", "BlinkMacSystemFont", "Helvetica Neue", "Segoe UI", "Arial"};
        for (String fName : fonts) { Font f = new Font(fName, style, size); if (f.getFamily().equals(fName)) return f; }
        return new Font("SansSerif", style, size);
    }
}

// ==========================================
// KOMPONEN UI "ULTIMATE MACOS"
// ==========================================
class PremiumButton extends JButton {
    public enum Style { PRIMARY, SECONDARY, DANGER, CALENDAR_DAY }
    private Style style;
    public PremiumButton(String text, Style style) {
        super(text); this.style = style;
        setFont(Theme.getFont(Font.BOLD, 15)); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false);
        setMargin(new Insets(0, 0, 0, 0)); setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(isEnabled()) repaint(); }
            public void mouseExited(MouseEvent e) { repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean isHovered = getModel().isRollover();
        Color bg = Theme.btnSecondary(), fg = Theme.text();
        
        if (style == Style.PRIMARY) { bg = isHovered ? Theme.btnPrimaryHover() : Theme.btnPrimary(); fg = Color.WHITE; }
        else if (style == Style.DANGER) { bg = isHovered ? Theme.btnSecondaryHover() : Theme.btnSecondary(); fg = Theme.btnDanger(); }
        else if (style == Style.SECONDARY) { bg = isHovered ? Theme.btnSecondaryHover() : Theme.btnSecondary(); }
        else if (style == Style.CALENDAR_DAY) { bg = isHovered ? Theme.accent() : Theme.cardBg(); fg = isHovered ? Color.WHITE : Theme.text(); }
        
        g2.setColor(!isEnabled() ? Theme.btnSecondary() : bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), style == Style.CALENDAR_DAY ? 8 : 16, style == Style.CALENDAR_DAY ? 8 : 16);
        setForeground(!isEnabled() ? Theme.textMuted() : fg);
        super.paintComponent(g); g2.dispose();
    }
}

class MenuSidebarButton extends JToggleButton {
    public MenuSidebarButton(String text) {
        super(text); setFont(Theme.getFont(Font.BOLD, 16)); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR)); setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(new EmptyBorder(14, 20, 14, 20)); addItemListener(e -> repaint());
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if(!isSelected()) repaint(); }
            public void mouseExited(MouseEvent e) { if(!isSelected()) repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isSelected() || getModel().isRollover()) { g2.setColor(Theme.btnSecondary()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); }
        setForeground(isSelected() ? Theme.accent() : (getModel().isRollover() ? Theme.text() : Theme.textMuted()));
        super.paintComponent(g); g2.dispose();
    }
}

class PremiumTextField extends JTextField {
    private boolean isFocused = false;
    public PremiumTextField() {
        setOpaque(false); setBorder(new EmptyBorder(14, 16, 14, 16)); setHorizontalAlignment(JTextField.CENTER);
        setFont(Theme.getFont(Font.PLAIN, 16)); setCaretColor(Theme.accent());
        setMaximumSize(new Dimension(340, 45)); setAlignmentX(Component.CENTER_ALIGNMENT);
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { isFocused = true; repaint(); }
            public void focusLost(FocusEvent e) { isFocused = false; repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.inputBg()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(isFocused ? Theme.accent() : Theme.border()); g2.setStroke(new BasicStroke(isFocused ? 2f : 1f));
        int offset = isFocused ? 1 : 0; g2.drawRoundRect(offset, offset, getWidth()-1-(offset*2), getHeight()-1-(offset*2), 12, 12);
        setForeground(Theme.text()); super.paintComponent(g); g2.dispose();
    }
}

class PremiumPasswordField extends JPasswordField {
    private boolean isFocused = false;
    public PremiumPasswordField() {
        setOpaque(false); setBorder(new EmptyBorder(14, 16, 14, 16)); setHorizontalAlignment(JTextField.CENTER);
        setFont(Theme.getFont(Font.PLAIN, 16)); setCaretColor(Theme.accent());
        setMaximumSize(new Dimension(340, 45)); setAlignmentX(Component.CENTER_ALIGNMENT);
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { isFocused = true; repaint(); }
            public void focusLost(FocusEvent e) { isFocused = false; repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.inputBg()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(isFocused ? Theme.accent() : Theme.border()); g2.setStroke(new BasicStroke(isFocused ? 2f : 1f));
        int offset = isFocused ? 1 : 0; g2.drawRoundRect(offset, offset, getWidth()-1-(offset*2), getHeight()-1-(offset*2), 12, 12);
        setForeground(Theme.text()); super.paintComponent(g); g2.dispose();
    }
}

class PremiumCard extends JPanel {
    public PremiumCard() { setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.cardBg()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        g2.setColor(Theme.border()); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 24, 24); 
        super.paintComponent(g); g2.dispose();
    }
}

class PCButton extends JButton {
    public int pcId; public String status, type; private String countdownStr = ""; private Color bgColor, fgColor, borderColor; public LocalDateTime endTime;
    public PCButton(int id, String type) {
        super(); this.pcId = id; this.type = type;
        setFont(Theme.getFont(Font.BOLD, 14)); setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR)); updateHTML();
    }
    public void setStatus(String status) {
        this.status = status;
        switch(status) {
            case "TERSEDIA": bgColor = Theme.pcAvailable(); fgColor = Theme.text(); borderColor = Theme.border(); break;
            case "AKTIF": bgColor = Theme.pcActive(); fgColor = Color.BLACK; borderColor = Theme.pcActive(); break;
            case "PERBAIKAN": bgColor = Theme.pcMaintenance(); fgColor = Color.BLACK; borderColor = Theme.pcMaintenance(); break;
            case "DIPESAN": bgColor = Theme.pcBooked(); fgColor = Color.WHITE; borderColor = Theme.pcBooked(); break; 
            default: bgColor = Theme.pcAvailable(); fgColor = Theme.text(); borderColor = Theme.border(); break;
        }
        setForeground(fgColor); updateHTML(); repaint();
    }
    public void setEndTime(LocalDateTime time) { this.endTime = time; }
    public void setCountdown(String timeStr) { this.countdownStr = timeStr; updateHTML(); }
    private void updateHTML() {
        if (status != null && status.equals("AKTIF") && !countdownStr.isEmpty()) {
            setText("<html><center>PC " + pcId + "<br><span style='font-size:10px; font-weight:normal;'>" + type + "</span><br><b style='font-size:11px;'>" + countdownStr + "</b></center></html>");
        } else if (status != null && status.equals("DIPESAN")) {
            setText("<html><center>PC " + pcId + "<br><span style='font-size:10px; font-weight:normal;'>" + type + "</span><br><b style='font-size:10px;'>Dipesen</b></center></html>");
        } else {
            setText("<html><center>PC " + pcId + "<br><span style='font-size:10px; font-weight:normal;'>" + type + "</span></center></html>");
        }
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if(status == null || status.equals("TERSEDIA")) { bgColor = Theme.pcAvailable(); fgColor = Theme.text(); borderColor = Theme.border(); }
        g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.setColor(borderColor); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
        super.paintComponent(g); g2.dispose();
    }
}

class PremiumCalendarDialog extends JDialog {
    private LocalDateTime selectedDateTime = null; private YearMonth currentYearMonth = YearMonth.now();
    private JPanel daysPanel; private JLabel lblMonthYear; private JComboBox<String> cbHour, cbMinute; private boolean isConfirmed = false;

    public PremiumCalendarDialog(JFrame parent) {
        super(parent, "Pilih Jadwal", true); setSize(380, 520); setLocationRelativeTo(parent); setUndecorated(true);
        JPanel mainPanel = new PremiumCard(); mainPanel.setLayout(new BorderLayout(0, 15)); mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel headerPanel = new JPanel(new BorderLayout()); headerPanel.setOpaque(false);
        lblMonthYear = new JLabel("", SwingConstants.CENTER); lblMonthYear.setFont(Theme.getFont(Font.BOLD, 18)); lblMonthYear.setForeground(Theme.text());
        PremiumButton btnPrev = new PremiumButton("<", PremiumButton.Style.SECONDARY); btnPrev.setPreferredSize(new Dimension(40, 30));
        PremiumButton btnNext = new PremiumButton(">", PremiumButton.Style.SECONDARY); btnNext.setPreferredSize(new Dimension(40, 30));
        btnPrev.addActionListener(e -> { currentYearMonth = currentYearMonth.minusMonths(1); updateCalendar(); });
        btnNext.addActionListener(e -> { currentYearMonth = currentYearMonth.plusMonths(1); updateCalendar(); });
        headerPanel.add(btnPrev, BorderLayout.WEST); headerPanel.add(lblMonthYear, BorderLayout.CENTER); headerPanel.add(btnNext, BorderLayout.EAST);
        
        JPanel bodyPanel = new JPanel(new BorderLayout()); bodyPanel.setOpaque(false);
        JPanel daysOfWeekPanel = new JPanel(new GridLayout(1, 7, 5, 5)); daysOfWeekPanel.setOpaque(false);
        String[] days = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
        for (String day : days) { JLabel d = new JLabel(day, SwingConstants.CENTER); d.setFont(Theme.getFont(Font.BOLD, 12)); d.setForeground(Theme.textMuted()); daysOfWeekPanel.add(d); }
        daysPanel = new JPanel(new GridLayout(6, 7, 5, 5)); daysPanel.setOpaque(false);
        bodyPanel.add(daysOfWeekPanel, BorderLayout.NORTH); bodyPanel.add(daysPanel, BorderLayout.CENTER);
        
        JPanel footerPanel = new JPanel(new BorderLayout(10, 10)); footerPanel.setOpaque(false);
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); timePanel.setOpaque(false);
        String[] hours = new String[24]; for(int i=0; i<24; i++) hours[i] = String.format("%02d", i);
        String[] minutes = new String[60]; for(int i=0; i<60; i++) minutes[i] = String.format("%02d", i);
        cbHour = new JComboBox<>(hours); styleTimeCombo(cbHour); cbHour.setSelectedItem(String.format("%02d", LocalDateTime.now().getHour()));
        cbMinute = new JComboBox<>(minutes); styleTimeCombo(cbMinute); cbMinute.setSelectedItem(String.format("%02d", LocalDateTime.now().getMinute()));
        JLabel colon = new JLabel(":"); colon.setForeground(Theme.text()); colon.setFont(Theme.getFont(Font.BOLD, 18));
        timePanel.add(new JLabel("Jam: ")); timePanel.add(cbHour); timePanel.add(colon); timePanel.add(cbMinute);
        
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0)); actionPanel.setOpaque(false);
        PremiumButton btnCancel = new PremiumButton("Batal", PremiumButton.Style.SECONDARY);
        PremiumButton btnPilih = new PremiumButton("Set Waktu", PremiumButton.Style.PRIMARY);
        btnCancel.addActionListener(e -> { isConfirmed = false; dispose(); });
        btnPilih.addActionListener(e -> {
            if(selectedDateTime == null) { JOptionPane.showMessageDialog(this, "Pilih tanggalnya dulu di kalender bos!"); return; }
            int h = Integer.parseInt((String)cbHour.getSelectedItem()); int m = Integer.parseInt((String)cbMinute.getSelectedItem());
            selectedDateTime = selectedDateTime.withHour(h).withMinute(m).withSecond(0).withNano(0);
            isConfirmed = true; dispose();
        });
        actionPanel.add(btnCancel); actionPanel.add(btnPilih);
        footerPanel.add(timePanel, BorderLayout.NORTH); footerPanel.add(actionPanel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH); mainPanel.add(bodyPanel, BorderLayout.CENTER); mainPanel.add(footerPanel, BorderLayout.SOUTH);
        getContentPane().setBackground(new Color(0,0,0,0)); setBackground(new Color(0,0,0,0));
        add(mainPanel); updateCalendar();
    }
    
    private void updateCalendar() {
        daysPanel.removeAll(); lblMonthYear.setText(currentYearMonth.getMonth().name() + " " + currentYearMonth.getYear());
        LocalDate firstOfMonth = currentYearMonth.atDay(1); int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; int daysInMonth = currentYearMonth.lengthOfMonth();
        for (int i = 0; i < dayOfWeek; i++) { daysPanel.add(new JLabel("")); }
        for (int i = 1; i <= daysInMonth; i++) {
            int day = i; PremiumButton btnDay = new PremiumButton(String.valueOf(day), PremiumButton.Style.CALENDAR_DAY); btnDay.setFont(Theme.getFont(Font.PLAIN, 14));
            if (selectedDateTime != null && selectedDateTime.toLocalDate().equals(currentYearMonth.atDay(day))) { btnDay.setForeground(Theme.accent()); btnDay.setFont(Theme.getFont(Font.BOLD, 15)); }
            btnDay.addActionListener(e -> { selectedDateTime = currentYearMonth.atDay(day).atStartOfDay(); updateCalendar(); });
            daysPanel.add(btnDay);
        }
        int remainingSlots = 42 - (dayOfWeek + daysInMonth);
        for (int i = 0; i < remainingSlots; i++) { daysPanel.add(new JLabel("")); }
        daysPanel.revalidate(); daysPanel.repaint();
    }
    
    private void styleTimeCombo(JComboBox<?> cb) {
        cb.setFont(Theme.getFont(Font.BOLD, 16)); cb.setPreferredSize(new Dimension(60, 35));
        ((JLabel)cb.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        cb.setUI(new BasicComboBoxUI() { @Override protected JButton createArrowButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; } });
    }
    public LocalDateTime getSelectedDateTime() { return isConfirmed ? selectedDateTime : null; }
}

// ==========================================
// KELAS UTAMA
// ==========================================
public class Warnet extends JFrame {

    String currentUser = ""; String currentUserRole = ""; 
    int savedReguler = 0, savedVip = 0, savedVvip = 0; 
    
    BookingPC currentBooking = null; LocalDateTime currentBookingDateTime = null;
    List<OrderItem> currentCart = new ArrayList<>(); List<MenuItem> menuList = new ArrayList<>();
    List<OrderItem> activeFoodCart = new ArrayList<>();
    List<PCButton> userBookPcButtons = new ArrayList<>(); List<PCButton> userActivatePcButtons = new ArrayList<>(); List<PCButton> adminPcButtons = new ArrayList<>();
    Timer dataSyncTimer;

    int selectedPcIdForUser = -1; int verifiedTransId = -1; String verifiedPcType = ""; int verifiedPcIdForAct = -1;

    CardLayout cardLayout; JPanel mainPanel, loginPanel, registerPanel, userPanel, adminPanel; JPanel scheduleListPanel;
    JLabel lblSavedTimeSidebar = new JLabel(); 
    NumberFormat idrFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    String[] specs = {
        "<html><center><span style='font-size:14px;'><b style='color:#0A84FF; font-size:16px;'>Spek Reguler:</b><br>AMD Ryzen 5 5600 &bull; RAM 16GB<br>RTX 3050 &bull; 24\" 75Hz</span></center></html>",
        "<html><center><span style='font-size:14px;'><b style='color:#0A84FF; font-size:16px;'>Spek VIP:</b><br>AMD Ryzen 7 5800X &bull; RAM 16GB<br>RTX 4060 &bull; 27\" 165Hz</span></center></html>",
        "<html><center><span style='font-size:14px;'><b style='color:#0A84FF; font-size:16px;'>Spek VVIP:</b><br>AMD Ryzen 9 7900X &bull; RAM 32GB<br>RTX 4070 Ti &bull; Dual 240Hz</span></center></html>"
    };

    public Warnet() { Database.setupTables(); initFrame(); }

    private void initFrame() {
        setTitle("MacSpace Premium"); setSize(1280, 850); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null);
        cardLayout = new CardLayout(); mainPanel = new JPanel(cardLayout); add(mainPanel);

        loginPanel = new JPanel(new GridBagLayout()); 
        registerPanel = new JPanel(new GridBagLayout()); 
        userPanel = new JPanel(new BorderLayout()); 
        adminPanel = new JPanel(new BorderLayout()); 

        mainPanel.add(loginPanel, "LOGIN"); mainPanel.add(registerPanel, "REGISTER");
        mainPanel.add(userPanel, "USER"); mainPanel.add(adminPanel, "ADMIN");

        buildLoginUI(); buildRegisterUI();
        applyGlobalTheme(this);
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void applyGlobalTheme(Component c) {
        if (c instanceof JFrame) { ((JFrame) c).getContentPane().setBackground(Theme.bg()); }
        if (c instanceof JPanel && !(c instanceof PremiumCard)) { c.setBackground(Theme.bg()); }
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            if (l.getName() != null && l.getName().equals("MUTED")) l.setForeground(Theme.textMuted());
            else if (l.getName() != null && l.getName().equals("TITLE")) l.setForeground(Theme.text());
            else if (l.getName() == null) l.setForeground(Theme.text()); 
        }
        if (c instanceof JScrollPane) {
            ((JScrollPane) c).getViewport().setBackground(c.getParent() instanceof PremiumCard ? Theme.cardBg() : Theme.bg());
            ((JScrollPane) c).setBorder(BorderFactory.createEmptyBorder());
        }
        if (c instanceof JComboBox) { c.setBackground(Theme.inputBg()); c.setForeground(Theme.text()); }
        if (c instanceof JRadioButton) { c.setForeground(Theme.text()); }
        if (c instanceof Container) { for (Component child : ((Container) c).getComponents()) applyGlobalTheme(child); }
        c.repaint();
    }

    public void toggleTheme() {
        Theme.isDark = !Theme.isDark; applyGlobalTheme(this); fetchPcStatuses(userBookPcButtons, userActivatePcButtons, adminPcButtons);
    }

    private JLabel createCenteredLabel(String text, String type, int size, int style) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER); lbl.setFont(Theme.getFont(style, size)); lbl.setName(type); lbl.setAlignmentX(Component.CENTER_ALIGNMENT); return lbl;
    }

    private JLabel createImageLabel(String path, int w, int h, String fallback) {
        try {
            Image img = null;
            java.net.URL imgURL = getClass().getResource("/" + path);
            if(imgURL != null) { img = new ImageIcon(imgURL).getImage(); } 
            else {
                java.io.File f = new java.io.File(path);
                if(f.exists()) { img = javax.imageio.ImageIO.read(f); }
            }
            if (img != null) {
                img = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                JLabel lbl = new JLabel(new ImageIcon(img)); lbl.setAlignmentX(Component.CENTER_ALIGNMENT); return lbl;
            }
        } catch (Exception e) {}
        
        JLabel fallbackLbl = new JLabel(fallback, SwingConstants.CENTER);
        fallbackLbl.setPreferredSize(new Dimension(w, h)); fallbackLbl.setMaximumSize(new Dimension(w, h));
        fallbackLbl.setBorder(BorderFactory.createDashedBorder(Theme.border(), 2, 2)); fallbackLbl.setForeground(Theme.textMuted()); fallbackLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return fallbackLbl;
    }

    private void showQRISDialog(double amount) {
        JPanel p = new JPanel(new BorderLayout(0, 15)); p.setOpaque(false);
        p.add(createCenteredLabel("Total Tagihan: " + idrFormat.format(amount), "TITLE", 20, Font.BOLD), BorderLayout.NORTH);
        p.add(createImageLabel("qris.png", 250, 250, "[ TARUH GAMBAR qris.png DI SINI ]"), BorderLayout.CENTER);
        p.add(createCenteredLabel("Scan pake m-Banking / Gopay / OVO / Dana", "MUTED", 14, Font.PLAIN), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, p, "Pembayaran QRIS", JOptionPane.PLAIN_MESSAGE);
    }

    private void fetchUserSavedTime() {
        if(currentUser.isEmpty()) return;
        try (Connection conn = Database.connect()) {
            PreparedStatement ps = conn.prepareStatement("SELECT saved_time_reguler, saved_time_vip, saved_time_vvip FROM users WHERE username = ?");
            ps.setString(1, currentUser);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                savedReguler = rs.getInt("saved_time_reguler");
                savedVip = rs.getInt("saved_time_vip");
                savedVvip = rs.getInt("saved_time_vvip");
                if (currentUserRole.equals("MEMBER")) {
                    lblSavedTimeSidebar.setText("<html><center>Tabungan Waktu:<br><span style='color:white; font-weight:normal;'>Reg: "+savedReguler+"m | VIP: "+savedVip+"m | VVIP: "+savedVvip+"m</span></center></html>");
                } else {
                    lblSavedTimeSidebar.setText(""); 
                }
            }
        } catch (SQLException e) {}
    }

    // ==========================================
    // UI LOGIN & REGISTER
    // ==========================================
    public void buildLoginUI() {
        loginPanel.removeAll();
        PremiumCard card = new PremiumCard(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(new EmptyBorder(60, 60, 60, 60));
        card.setPreferredSize(new Dimension(450, 650));

        JLabel lblLogo = createImageLabel("logo.png", 90, 90, "LOGO");
        JLabel lblTitle = createCenteredLabel("MacSpace", "TITLE", 40, Font.BOLD);
        JLabel lblInfo = createCenteredLabel("Selamat datang kembali, bosque", "MUTED", 16, Font.PLAIN);

        PremiumTextField txtUser = new PremiumTextField(); PremiumPasswordField txtPass = new PremiumPasswordField(); 

        PremiumButton btnLogin = new PremiumButton("Masuk", PremiumButton.Style.PRIMARY); btnLogin.setMaximumSize(new Dimension(340, 45)); btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton btnGoRegister = new JButton("Belum ada akun? Daftar sini..."); btnGoRegister.setForeground(Theme.btnPrimary()); btnGoRegister.setFont(Theme.getFont(Font.PLAIN, 15)); btnGoRegister.setContentAreaFilled(false); btnGoRegister.setBorderPainted(false); btnGoRegister.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnGoRegister.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUser.addActionListener(e -> btnLogin.doClick()); txtPass.addActionListener(e -> btnLogin.doClick());

        card.add(Box.createVerticalGlue());
        card.add(lblLogo); card.add(Box.createVerticalStrut(15));
        card.add(lblTitle); card.add(Box.createVerticalStrut(10)); card.add(lblInfo); card.add(Box.createVerticalStrut(45));
        card.add(createCenteredLabel("Username", "MUTED", 14, Font.BOLD)); card.add(Box.createVerticalStrut(10)); card.add(txtUser); card.add(Box.createVerticalStrut(25));
        card.add(createCenteredLabel("Password", "MUTED", 14, Font.BOLD)); card.add(Box.createVerticalStrut(10)); card.add(txtPass); card.add(Box.createVerticalStrut(45));
        card.add(btnLogin); card.add(Box.createVerticalStrut(15)); card.add(btnGoRegister);
        card.add(Box.createVerticalGlue());

        btnLogin.addActionListener(e -> {
            try {
                String u = txtUser.getText().trim(); String p = new String(txtPass.getPassword()).trim();
                if (u.isEmpty() || p.isEmpty()) throw new ValidasiException("Isi dulu semuanya bos!");
                if (u.length() < 3) throw new ValidasiException("Username minimal 3 huruf ya.");
                processLogin(u, p, txtUser, txtPass);
            } catch (ValidasiException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Perhatian", JOptionPane.WARNING_MESSAGE); }
        });
        btnGoRegister.addActionListener(e -> { txtUser.setText(""); txtPass.setText(""); cardLayout.show(mainPanel, "REGISTER"); applyGlobalTheme(this); });
        loginPanel.add(card, new GridBagConstraints()); 
    }

    public void buildRegisterUI() {
        registerPanel.removeAll();
        PremiumCard card = new PremiumCard(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); card.setBorder(new EmptyBorder(50, 60, 50, 60));
        card.setPreferredSize(new Dimension(450, 680));

        JLabel lblLogo = createImageLabel("logo.png", 70, 70, "LOGO");
        JLabel lblTitle = createCenteredLabel("Daftar Member", "TITLE", 36, Font.BOLD);
        JLabel lblSub = createCenteredLabel("Jadi Member buat dapet fitur Token", "MUTED", 14, Font.PLAIN);

        PremiumTextField txtUser = new PremiumTextField(); PremiumPasswordField txtPass = new PremiumPasswordField(); 

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); radioPanel.setOpaque(false); radioPanel.setMaximumSize(new Dimension(340, 45)); radioPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JRadioButton rbTunai = new JRadioButton("Tunai", true); rbTunai.setFont(Theme.getFont(Font.PLAIN, 15)); rbTunai.setOpaque(false);
        JRadioButton rbQris = new JRadioButton("Pake QRIS"); rbQris.setFont(Theme.getFont(Font.PLAIN, 15)); rbQris.setOpaque(false);
        ButtonGroup bgPayment = new ButtonGroup(); bgPayment.add(rbTunai); bgPayment.add(rbQris); radioPanel.add(rbTunai); radioPanel.add(rbQris);

        PremiumButton btnRegister = new PremiumButton("Daftar Sekarang", PremiumButton.Style.PRIMARY); btnRegister.setMaximumSize(new Dimension(340, 45)); btnRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton btnGoLogin = new JButton("Udah punya akun? Login aja."); btnGoLogin.setForeground(Theme.btnPrimary()); btnGoLogin.setFont(Theme.getFont(Font.PLAIN, 15)); btnGoLogin.setContentAreaFilled(false); btnGoLogin.setBorderPainted(false); btnGoLogin.setCursor(new Cursor(Cursor.HAND_CURSOR)); btnGoLogin.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtUser.addActionListener(e -> btnRegister.doClick()); txtPass.addActionListener(e -> btnRegister.doClick());

        card.add(Box.createVerticalGlue());
        card.add(lblLogo); card.add(Box.createVerticalStrut(10));
        card.add(lblTitle); card.add(Box.createVerticalStrut(10)); card.add(lblSub); card.add(Box.createVerticalStrut(30));
        card.add(createCenteredLabel("Username", "MUTED", 14, Font.BOLD)); card.add(Box.createVerticalStrut(10)); card.add(txtUser); card.add(Box.createVerticalStrut(20));
        card.add(createCenteredLabel("Password", "MUTED", 14, Font.BOLD)); card.add(Box.createVerticalStrut(10)); card.add(txtPass); card.add(Box.createVerticalStrut(20));
        card.add(createCenteredLabel("Harga Member (Rp 50.000)", "MUTED", 14, Font.BOLD)); card.add(Box.createVerticalStrut(10)); card.add(radioPanel); card.add(Box.createVerticalStrut(30));
        card.add(btnRegister); card.add(Box.createVerticalStrut(15)); card.add(btnGoLogin);
        card.add(Box.createVerticalGlue());

        btnRegister.addActionListener(e -> {
            String u = txtUser.getText().trim(); String p = new String(txtPass.getPassword()).trim(); String method = rbTunai.isSelected() ? "Tunai" : "QRIS";
            if(u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Data belum lengkap!"); return; }
            if(method.equals("QRIS")) { showQRISDialog(50000); }
            processRegister(u, p, method, txtUser, txtPass);
        });
        btnGoLogin.addActionListener(e -> { txtUser.setText(""); txtPass.setText(""); cardLayout.show(mainPanel, "LOGIN"); applyGlobalTheme(this); });
        registerPanel.add(card, new GridBagConstraints());
    }

    private void processLogin(String u, String p, JTextField txtU, JPasswordField txtP) {
        try (Connection conn = Database.connect()) {
            if (conn == null) return;
            try (PreparedStatement pst = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?")) {
                pst.setString(1, u); pst.setString(2, p); ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    currentUser = rs.getString("username"); currentUserRole = rs.getString("role");
                    if (currentUserRole == null) currentUserRole = "USER";

                    txtU.setText(""); txtP.setText("");
                    if (currentUserRole.equalsIgnoreCase("USER") || currentUserRole.equalsIgnoreCase("MEMBER")) { 
                        loadMenuFromDB(); fetchUserSavedTime(); buildUserUI(); cardLayout.show(mainPanel, "USER"); applyGlobalTheme(this); 
                    }
                    else if (currentUserRole.equalsIgnoreCase("ADMIN")) { buildAdminUI(); cardLayout.show(mainPanel, "ADMIN"); applyGlobalTheme(this); }
                } else { JOptionPane.showMessageDialog(this, "Username atau password salah.", "Error", JOptionPane.ERROR_MESSAGE); }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void processRegister(String u, String p, String payMethod, JTextField txtU, JPasswordField txtP) {
        try (Connection conn = Database.connect()) {
            if (conn == null) return;
            try (PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username=?")) {
                check.setString(1, u); if (check.executeQuery().next()) { JOptionPane.showMessageDialog(this, "Username udah dipake."); return; }
            }
            try (PreparedStatement insert = conn.prepareStatement("INSERT INTO users (username, password, role, saved_time_reguler, saved_time_vip, saved_time_vvip) VALUES (?, ?, 'MEMBER', 0, 0, 0)")) { 
                insert.setString(1, u); insert.setString(2, p); insert.executeUpdate(); 
            }
            try (PreparedStatement trans = conn.prepareStatement("INSERT INTO transactions (customer_name, pc_type, duration, duration_minutes, total_amount, status, payment_method, is_activated) VALUES (?, 'User Baru', 0, 0, 50000, 'SELESAI', ?, 1)")) { 
                trans.setString(1, u); trans.setString(2, payMethod); trans.executeUpdate(); 
            }
            JOptionPane.showMessageDialog(this, "Akun Member berhasil dibuat! Silakan Login."); 
            
            SwingUtilities.invokeLater(() -> { txtU.setText(""); txtP.setText(""); cardLayout.show(mainPanel, "LOGIN"); applyGlobalTheme(this); });
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void loadMenuFromDB() {
        menuList.clear();
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM menu")) {
            while (rs.next()) { menuList.add(new MenuItem(rs.getInt("id"), rs.getString("name"), rs.getString("type"), rs.getDouble("price"))); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ==========================================
    // UI USER / MEMBER
    // ==========================================
    public void buildUserUI() {
        userPanel.removeAll(); userBookPcButtons.clear(); userActivatePcButtons.clear();
        selectedPcIdForUser = -1; verifiedTransId = -1; verifiedPcType = ""; verifiedPcIdForAct = -1;

        // SIDEBAR KIRI
        JPanel sidebarPanel = new JPanel(); sidebarPanel.setPreferredSize(new Dimension(280, getHeight()));
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS)); sidebarPanel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel lblLogo = createImageLabel("logo.png", 60, 60, "LOGO");
        JLabel lblWelcome = createCenteredLabel("Hi, " + currentUser, "TITLE", 26, Font.BOLD);
        
        JLabel lblRole = createCenteredLabel(currentUserRole.toUpperCase(), "MUTED", 12, Font.BOLD);
        lblRole.setForeground(currentUserRole.equals("MEMBER") ? Theme.pcActive() : Theme.textMuted());

        lblSavedTimeSidebar = createCenteredLabel("", "MUTED", 14, Font.BOLD);
        lblSavedTimeSidebar.setForeground(Theme.accent());
        fetchUserSavedTime();

        JLabel lblClock = createCenteredLabel("", "MUTED", 14, Font.PLAIN);
        Timer timer = new Timer(1000, e -> lblClock.setText(new SimpleDateFormat("E, d MMM  HH:mm").format(new Date()))); timer.start();

        JPanel navMenuPanel = new JPanel(); navMenuPanel.setLayout(new BoxLayout(navMenuPanel, BoxLayout.Y_AXIS)); navMenuPanel.setOpaque(false);
        MenuSidebarButton btnNavBook = new MenuSidebarButton("Mulai Main"); btnNavBook.setMaximumSize(new Dimension(220, 45)); btnNavBook.setAlignmentX(Component.CENTER_ALIGNMENT);
        MenuSidebarButton btnNavSched = new MenuSidebarButton("Jadwal "); btnNavSched.setMaximumSize(new Dimension(220, 45)); btnNavSched.setAlignmentX(Component.CENTER_ALIGNMENT);
        MenuSidebarButton btnNavAct = new MenuSidebarButton("Aktivasi Token"); btnNavAct.setMaximumSize(new Dimension(220, 45)); btnNavAct.setAlignmentX(Component.CENTER_ALIGNMENT);
        MenuSidebarButton btnNavFood = new MenuSidebarButton("Pesan F&B"); btnNavFood.setMaximumSize(new Dimension(220, 45)); btnNavFood.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        ButtonGroup navGroup = new ButtonGroup(); navGroup.add(btnNavBook); navGroup.add(btnNavSched); navGroup.add(btnNavAct); navGroup.add(btnNavFood); btnNavBook.setSelected(true);

        navMenuPanel.add(btnNavBook); navMenuPanel.add(Box.createVerticalStrut(5));
        navMenuPanel.add(btnNavSched); navMenuPanel.add(Box.createVerticalStrut(5));
        navMenuPanel.add(btnNavAct); navMenuPanel.add(Box.createVerticalStrut(5));
        navMenuPanel.add(btnNavFood);

        PremiumButton btnRefreshData = new PremiumButton("Refresh Data", PremiumButton.Style.SECONDARY); btnRefreshData.setMaximumSize(new Dimension(220, 45)); btnRefreshData.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRefreshData.addActionListener(e -> { fetchPcStatuses(userBookPcButtons, userActivatePcButtons); refreshSchedule(); fetchUserSavedTime(); });

        PremiumButton btnToggleTheme = new PremiumButton("Ganti Tema", PremiumButton.Style.SECONDARY); btnToggleTheme.setMaximumSize(new Dimension(220, 45)); btnToggleTheme.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnToggleTheme.addActionListener(e -> toggleTheme());
        PremiumButton btnLogout = new PremiumButton("Keluar", PremiumButton.Style.DANGER); btnLogout.setMaximumSize(new Dimension(220, 45)); btnLogout.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogout.addActionListener(e -> { 
            timer.stop(); stopDataSyncTimer(); currentUser = ""; currentUserRole = ""; activeFoodCart.clear();
            cardLayout.show(mainPanel, "LOGIN"); applyGlobalTheme(this); 
        });

        sidebarPanel.add(lblLogo); sidebarPanel.add(Box.createVerticalStrut(15));
        sidebarPanel.add(lblWelcome); sidebarPanel.add(Box.createVerticalStrut(5)); sidebarPanel.add(lblRole); sidebarPanel.add(Box.createVerticalStrut(5)); sidebarPanel.add(lblClock);
        sidebarPanel.add(Box.createVerticalStrut(15)); sidebarPanel.add(lblSavedTimeSidebar);
        sidebarPanel.add(Box.createVerticalStrut(20));
        sidebarPanel.add(navMenuPanel); 
        sidebarPanel.add(Box.createVerticalGlue());
        sidebarPanel.add(btnRefreshData); sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnToggleTheme); sidebarPanel.add(Box.createVerticalStrut(10)); sidebarPanel.add(btnLogout);

        // KONTEN KANAN
        CardLayout userContentLayout = new CardLayout(); JPanel userContentPanel = new JPanel(userContentLayout); 

        // TAB 1: PEMESANAN 
        CardLayout bookFlowLayout = new CardLayout(); JPanel bookContentPanel = new JPanel(bookFlowLayout);
        
        JPanel step1Panel = new JPanel(new BorderLayout(40, 0)); step1Panel.setOpaque(false); step1Panel.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        PremiumCard formCard = new PremiumCard(); formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS)); formCard.setBorder(new EmptyBorder(35, 35, 35, 35));
        formCard.setPreferredSize(new Dimension(380, 0));

        JLabel lblPcTitle = createCenteredLabel("Setup Main", "TITLE", 28, Font.BOLD);
        
        JComboBox<String> cbMode = new JComboBox<>();
        cbMode.addItem("Aktifin Sekarang");
        cbMode.addItem("Booking dulu");
        if (currentUserRole.equals("MEMBER")) {
            cbMode.addItem("Beli Token");
            cbMode.addItem("Pakai Sisa Waktu"); 
        }
        styleComboBox(cbMode);
        
        JLabel lblDateTitle = createCenteredLabel("Tanggal & Jam", "MUTED", 14, Font.BOLD); lblDateTitle.setVisible(false);
        PremiumButton btnShowCalendar = new PremiumButton("Pilih Waktu Main...", PremiumButton.Style.SECONDARY);
        btnShowCalendar.setMaximumSize(new Dimension(340, 45)); btnShowCalendar.setAlignmentX(Component.CENTER_ALIGNMENT); btnShowCalendar.setVisible(false);

        btnShowCalendar.addActionListener(e -> {
            PremiumCalendarDialog calDialog = new PremiumCalendarDialog(Warnet.this); calDialog.setVisible(true);
            LocalDateTime dt = calDialog.getSelectedDateTime();
            if(dt != null) { currentBookingDateTime = dt; btnShowCalendar.setText(dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))); btnShowCalendar.setForeground(Theme.accent()); }
        });

        JLabel lblSelectedPc = createCenteredLabel("Pilih dulu PC di denah...", "TITLE", 16, Font.PLAIN); lblSelectedPc.setForeground(Theme.btnPrimary());

        String[] paketPc = {"Reguler - Rp 15k/jam", "VIP - Rp 25k/jam", "VVIP - Rp 35k/jam"}; int[] hargaPc = {15000, 25000, 35000};
        JComboBox<String> cbPaket = new JComboBox<>(paketPc); styleComboBox(cbPaket); cbPaket.setEnabled(false);
        
        JLabel lblDurasiTitle = createCenteredLabel("Berapa Jam?", "MUTED", 14, Font.BOLD);
        PremiumTextField txtDurasi = new PremiumTextField(); txtDurasi.setText("1");
        PremiumButton btnNext = new PremiumButton("Lanjut", PremiumButton.Style.PRIMARY); btnNext.setMaximumSize(new Dimension(340, 45)); btnNext.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtDurasi.addActionListener(e -> btnNext.doClick());

        JLabel lblSpecs = createCenteredLabel(specs[0], "MUTED", 14, Font.PLAIN);
        
        formCard.add(Box.createVerticalGlue());
        formCard.add(lblPcTitle); formCard.add(Box.createVerticalStrut(30));
        formCard.add(createCenteredLabel("Mau Gimana?", "MUTED", 14, Font.BOLD)); formCard.add(Box.createVerticalStrut(10)); formCard.add(cbMode); formCard.add(Box.createVerticalStrut(20));
        formCard.add(lblDateTitle); formCard.add(Box.createVerticalStrut(10)); formCard.add(btnShowCalendar); formCard.add(Box.createVerticalStrut(15));
        formCard.add(createCenteredLabel("PC Lo", "MUTED", 14, Font.BOLD)); formCard.add(Box.createVerticalStrut(10)); formCard.add(lblSelectedPc); formCard.add(Box.createVerticalStrut(20));
        formCard.add(createCenteredLabel("Kelas PC", "MUTED", 14, Font.BOLD)); formCard.add(Box.createVerticalStrut(10)); formCard.add(cbPaket); formCard.add(Box.createVerticalStrut(15));
        formCard.add(lblSpecs); formCard.add(Box.createVerticalStrut(25));
        formCard.add(lblDurasiTitle); formCard.add(Box.createVerticalStrut(10)); formCard.add(txtDurasi); formCard.add(Box.createVerticalStrut(40));
        formCard.add(btnNext);
        formCard.add(Box.createVerticalGlue());

        PremiumCard gridCard1 = new PremiumCard(); gridCard1.setLayout(new BorderLayout()); gridCard1.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel monitorHeader1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); monitorHeader1.setOpaque(false);
        monitorHeader1.add(createLegendColor("Tersedia", Theme.pcAvailable())); monitorHeader1.add(createLegendColor("Nyala", Theme.pcActive()));
        monitorHeader1.add(createLegendColor("Perbaikan", Theme.pcMaintenance())); monitorHeader1.add(createLegendColor("Dipesan", Theme.pcBooked()));
        gridCard1.add(monitorHeader1, BorderLayout.NORTH);

        JPanel floorsPanel1 = new JPanel(); floorsPanel1.setLayout(new BoxLayout(floorsPanel1, BoxLayout.Y_AXIS)); floorsPanel1.setOpaque(false);
        ActionListener bookPcListener = e -> {
            String mod = (String) cbMode.getSelectedItem();
            if("Beli Token".equals(mod)) return; 
            PCButton btn = (PCButton) e.getSource();
            
            // FIX: Bisa nge-klik PC kalo mau Jadwalin Aja walau lagi dipake!
            if (!btn.status.equals("TERSEDIA") && !"Jadwalin Aja".equals(mod)) { 
                JOptionPane.showMessageDialog(this, "PC ini lagi " + btn.status + " boss! Kalo mau, pilih opsi Jadwalin Aja."); return; 
            }
            
            for(PCButton b : userBookPcButtons) b.setBorder(null);
            btn.setBorder(BorderFactory.createLineBorder(Theme.accent(), 3, true)); selectedPcIdForUser = btn.pcId;
            lblSelectedPc.setText("PC " + btn.pcId + " (" + btn.type + ")"); lblSelectedPc.setForeground(Theme.pcActive());
            
            if (btn.type.equals("Reguler")) { cbPaket.setSelectedIndex(0); lblSpecs.setText(specs[0]); }
            else if (btn.type.equals("VIP")) { cbPaket.setSelectedIndex(1); lblSpecs.setText(specs[1]); }
            else { cbPaket.setSelectedIndex(2); lblSpecs.setText(specs[2]); }
            
            if ("Pakai Sisa Waktu".equals(mod)) {
                int maxSisa = btn.type.equals("Reguler") ? savedReguler : (btn.type.equals("VIP") ? savedVip : savedVvip);
                lblDurasiTitle.setText("Pakai Berapa Menit? (Max: " + maxSisa + ")");
                txtDurasi.setText(String.valueOf(maxSisa > 0 ? maxSisa : 0));
            }
        };

        floorsPanel1.add(createFloorPanel("Lantai 1 \u2014 Reguler", 1, 10, "Reguler", userBookPcButtons, bookPcListener));
        floorsPanel1.add(createFloorPanel("Lantai 2 \u2014 VIP", 11, 20, "VIP", userBookPcButtons, bookPcListener));
        floorsPanel1.add(createFloorPanel("Lantai 3 \u2014 VVIP", 21, 30, "VVIP", userBookPcButtons, bookPcListener));
        gridCard1.add(new JScrollPane(floorsPanel1), BorderLayout.CENTER);

        cbPaket.addActionListener(e -> { 
            String mod = (String) cbMode.getSelectedItem();
            if("Beli Token".equals(mod) && cbPaket.getSelectedIndex() < specs.length) lblSpecs.setText(specs[cbPaket.getSelectedIndex()]); 
        });

        cbMode.addActionListener(e -> {
            String mod = (String) cbMode.getSelectedItem();
            if(mod == null) return;
            boolean isJadwal = "Booking dulu".equals(mod);
            boolean isToken = "Beli Token".equals(mod);
            boolean isSimpan = "Pakai Sisa Waktu".equals(mod);
            
            lblDateTitle.setVisible(isJadwal); btnShowCalendar.setVisible(isJadwal);

            if(isToken) { 
                cbPaket.setEnabled(true); lblSelectedPc.setText("Token buat PC yang lo pilih."); lblSelectedPc.setForeground(Theme.textMuted());
                selectedPcIdForUser = -1; for(PCButton b : userBookPcButtons) b.setBorder(null); btnNext.setText("Ke Pembayaran");
                lblDurasiTitle.setText("Berapa Jam?"); txtDurasi.setText("1");
            } else if (isSimpan) {
                fetchUserSavedTime(); 
                cbPaket.setEnabled(false); lblSelectedPc.setText("Pilih dulu PC di denah..."); lblSelectedPc.setForeground(Theme.accent());
                selectedPcIdForUser = -1; for(PCButton b : userBookPcButtons) b.setBorder(null); btnNext.setText("Gaskeun Pake Sisa");
                lblDurasiTitle.setText("Pilih PC dulu di kanan!"); txtDurasi.setText("0");
            } else { 
                cbPaket.setEnabled(false); lblSelectedPc.setText("Pilih dulu PC di denah..."); lblSelectedPc.setForeground(Theme.accent());
                selectedPcIdForUser = -1; for(PCButton b : userBookPcButtons) b.setBorder(null); btnNext.setText("Lanjut");
                lblDurasiTitle.setText("Berapa Jam?"); txtDurasi.setText("1");
            }
        });

        step1Panel.add(formCard, BorderLayout.WEST); 
        step1Panel.add(gridCard1, BorderLayout.CENTER);

        // STEP 2: F&B
        JPanel step2Panel = new JPanel(new BorderLayout()); step2Panel.setBorder(new EmptyBorder(40, 40, 40, 40)); step2Panel.setOpaque(false);
        PremiumCard fbCard = new PremiumCard(); fbCard.setLayout(new BoxLayout(fbCard, BoxLayout.Y_AXIS)); fbCard.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel lblFbTitle = createCenteredLabel("Jajan Dulu? (Opsional)", "TITLE", 28, Font.BOLD);
        JComboBox<String> cbMenu = new JComboBox<>(); for(MenuItem m : menuList) cbMenu.addItem(m.name + " (" + idrFormat.format(m.price) + ")"); styleComboBox(cbMenu);
        PremiumTextField txtQty = new PremiumTextField(); txtQty.setText("1"); txtQty.setMaximumSize(new Dimension(150, 45));
        PremiumButton btnAddCart = new PremiumButton("Tambah", PremiumButton.Style.SECONDARY); btnAddCart.setPreferredSize(new Dimension(100, 45));
        PremiumButton btnRemoveCart = new PremiumButton("Hapus", PremiumButton.Style.DANGER); btnRemoveCart.setPreferredSize(new Dimension(80, 45));

        txtQty.addActionListener(e -> btnAddCart.doClick());

        DefaultTableModel cartModel = new DefaultTableModel(new String[]{"Menu", "Jumlah", "Total"}, 0);
        JTable cartTable = createModernTable(cartModel); JScrollPane scrollCart = new JScrollPane(cartTable); 

        JPanel payActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); payActionPanel.setOpaque(false);
        JLabel lblTotal = createCenteredLabel("Total: Rp 0", "TITLE", 24, Font.BOLD);
        PremiumButton btnBack = new PremiumButton("Balik", PremiumButton.Style.SECONDARY); btnBack.setPreferredSize(new Dimension(110, 45));
        PremiumButton btnConfirm = new PremiumButton("Bayar & Proses", PremiumButton.Style.PRIMARY); btnConfirm.setPreferredSize(new Dimension(180, 45));
        JRadioButton rbTunaiPay = new JRadioButton("Tunai", true); rbTunaiPay.setFont(Theme.getFont(Font.PLAIN,15)); rbTunaiPay.setOpaque(false);
        JRadioButton rbQrisPay = new JRadioButton("Pake QRIS"); rbQrisPay.setFont(Theme.getFont(Font.PLAIN,15)); rbQrisPay.setOpaque(false);
        ButtonGroup bgPay = new ButtonGroup(); bgPay.add(rbTunaiPay); bgPay.add(rbQrisPay);
        payActionPanel.add(btnBack); payActionPanel.add(rbTunaiPay); payActionPanel.add(rbQrisPay); payActionPanel.add(btnConfirm);

        fbCard.add(lblFbTitle); fbCard.add(Box.createVerticalStrut(30));
        JPanel inputFbRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); inputFbRow.setOpaque(false);
        cbMenu.setPreferredSize(new Dimension(250, 45)); inputFbRow.add(cbMenu); inputFbRow.add(txtQty); inputFbRow.add(btnAddCart); inputFbRow.add(btnRemoveCart);
        fbCard.add(inputFbRow); fbCard.add(Box.createVerticalStrut(30));
        fbCard.add(scrollCart); fbCard.add(Box.createVerticalStrut(30));
        fbCard.add(lblTotal); fbCard.add(Box.createVerticalStrut(20));
        
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); btnWrapper.setOpaque(false); btnWrapper.add(payActionPanel);
        fbCard.add(btnWrapper);
        
        step2Panel.add(fbCard, BorderLayout.CENTER);

        bookContentPanel.add(step1Panel, "STEP1"); bookContentPanel.add(step2Panel, "STEP2");
        userContentPanel.add(bookContentPanel, "BOOK");

        // TAB JADWAL & TOKEN
        JPanel schedulePanel = new JPanel(new BorderLayout()); schedulePanel.setBorder(new EmptyBorder(40, 40, 40, 40)); schedulePanel.setOpaque(false);
        PremiumCard scHolderCard = new PremiumCard(); scHolderCard.setLayout(new BorderLayout()); scHolderCard.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        JLabel lblSchedTitle = createCenteredLabel("Jadwal, Token & PC Aktif", "TITLE", 30, Font.BOLD); lblSchedTitle.setBorder(new EmptyBorder(0,0,25,0));
        scheduleListPanel = new JPanel(); scheduleListPanel.setLayout(new BoxLayout(scheduleListPanel, BoxLayout.Y_AXIS)); scheduleListPanel.setOpaque(false);
        scHolderCard.add(lblSchedTitle, BorderLayout.NORTH); scHolderCard.add(new JScrollPane(scheduleListPanel), BorderLayout.CENTER);
        
        schedulePanel.add(scHolderCard, BorderLayout.CENTER);
        userContentPanel.add(schedulePanel, "SCHED");

        // TAB AKTIVASI
        JPanel activatePanel = new JPanel(new BorderLayout()); activatePanel.setBorder(new EmptyBorder(40, 40, 40, 40)); activatePanel.setOpaque(false);
        PremiumCard actCard = new PremiumCard(); actCard.setLayout(new BorderLayout(0, 20)); actCard.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel verifyTop = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); verifyTop.setOpaque(false);
        PremiumTextField txtCode = new PremiumTextField(); txtCode.setMaximumSize(new Dimension(250, 45)); txtCode.setPreferredSize(new Dimension(250, 45));
        PremiumButton btnVerify = new PremiumButton("Cek Kode", PremiumButton.Style.PRIMARY); btnVerify.setPreferredSize(new Dimension(130, 45));
        
        txtCode.addActionListener(e -> btnVerify.doClick());

        verifyTop.add(txtCode); verifyTop.add(btnVerify);
        
        JLabel lblVerifyStatus = createCenteredLabel("Masukin kode aktivasi token lu di atas.", "MUTED", 15, Font.PLAIN);

        JPanel monitorHeader2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); monitorHeader2.setOpaque(false);
        monitorHeader2.add(createLegendColor("Tersedia", Theme.pcAvailable())); monitorHeader2.add(createLegendColor("Nyala", Theme.pcActive()));
        monitorHeader2.add(createLegendColor("Perbaikan", Theme.pcMaintenance())); monitorHeader2.add(createLegendColor("Dipesan", Theme.pcBooked()));

        JPanel floorsPanel2 = new JPanel(); floorsPanel2.setLayout(new BoxLayout(floorsPanel2, BoxLayout.Y_AXIS)); floorsPanel2.setOpaque(false);
        
        ActionListener verifyPcListener = e -> {
            PCButton btn = (PCButton) e.getSource();
            if(verifiedTransId == -1) { JOptionPane.showMessageDialog(this, "Cek kode dulu."); return; }
            if (verifiedPcIdForAct > 0) { 
                if (btn.pcId != verifiedPcIdForAct) { JOptionPane.showMessageDialog(this, "Woi pinter, kode ini khusus PC " + verifiedPcIdForAct + "yaa!!"); return; }
                if (btn.status.equals("AKTIF") || btn.status.equals("PERBAIKAN")) { JOptionPane.showMessageDialog(this, "PC gabisa dipake."); return; }
            } else { 
                if(!btn.type.equals(verifiedPcType)) { JOptionPane.showMessageDialog(this, "Cuma bisa dipake buat PC kelas " + verifiedPcType + " boss!"); return; }
                if(!btn.status.equals("TERSEDIA")) { JOptionPane.showMessageDialog(this, "PC lagi dipake."); return; }
            }

            if(JOptionPane.showConfirmDialog(this, "Main di PC " + btn.pcId + "?", "Aktivasi", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try (Connection conn = Database.connect()) {
                    if (conn == null) return;
                    PreparedStatement psTrans = conn.prepareStatement("UPDATE transactions SET status = 'AKTIF', is_activated = 1, pc_id = ?, activated_at = CURRENT_TIMESTAMP WHERE id = ?");
                    psTrans.setInt(1, btn.pcId); psTrans.setInt(2, verifiedTransId); psTrans.executeUpdate();
                    
                    PreparedStatement psPc = conn.prepareStatement("UPDATE pcs SET status = 'AKTIF' WHERE id = ?");
                    psPc.setInt(1, btn.pcId); psPc.executeUpdate();
                    
                    verifiedTransId = -1; verifiedPcType = ""; verifiedPcIdForAct = -1; txtCode.setText(""); lblVerifyStatus.setText("Berhasil aktivasi!");
                    fetchPcStatuses(userBookPcButtons, userActivatePcButtons); refreshSchedule();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        };

        floorsPanel2.add(createFloorPanel("Lantai 1 \u2014 Reguler", 1, 10, "Reguler", userActivatePcButtons, verifyPcListener));
        floorsPanel2.add(createFloorPanel("Lantai 2 \u2014 VIP", 11, 20, "VIP", userActivatePcButtons, verifyPcListener));
        floorsPanel2.add(createFloorPanel("Lantai 3 \u2014 VVIP", 21, 30, "VVIP", userActivatePcButtons, verifyPcListener));
        
        JPanel actContent = new JPanel(new BorderLayout()); actContent.setOpaque(false);
        actContent.add(monitorHeader2, BorderLayout.NORTH); actContent.add(new JScrollPane(floorsPanel2), BorderLayout.CENTER);

        actCard.add(verifyTop, BorderLayout.NORTH);
        
        JPanel lblStatusWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); lblStatusWrapper.setOpaque(false); lblStatusWrapper.add(lblVerifyStatus);
        actCard.add(lblStatusWrapper, BorderLayout.SOUTH);
        actCard.add(actContent, BorderLayout.CENTER);
        activatePanel.add(actCard, BorderLayout.CENTER);
        userContentPanel.add(activatePanel, "ACT");

        // TAB FOOD
        JPanel foodPanel = new JPanel(new BorderLayout()); foodPanel.setBorder(new EmptyBorder(40, 40, 40, 40)); foodPanel.setOpaque(false);
        PremiumCard fCard = new PremiumCard(); fCard.setLayout(new BoxLayout(fCard, BoxLayout.Y_AXIS)); fCard.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel lblFoodTitle = createCenteredLabel("Pesan Makanan ke Meja Lo", "TITLE", 28, Font.BOLD);
        JComboBox<ActivePCItem> cbActivePc = new JComboBox<>(); styleComboBox(cbActivePc);
        JComboBox<String> cbFoodMenu = new JComboBox<>(); for(MenuItem m : menuList) cbFoodMenu.addItem(m.name + " (" + idrFormat.format(m.price) + ")"); styleComboBox(cbFoodMenu);
        PremiumTextField txtFoodQty = new PremiumTextField(); txtFoodQty.setText("1"); txtFoodQty.setMaximumSize(new Dimension(100, 45));
        PremiumButton btnAddFood = new PremiumButton("Tambah", PremiumButton.Style.SECONDARY); btnAddFood.setPreferredSize(new Dimension(100, 45));
        PremiumButton btnRemoveFood = new PremiumButton("Hapus", PremiumButton.Style.DANGER); btnRemoveFood.setPreferredSize(new Dimension(80, 45));

        txtFoodQty.addActionListener(e -> btnAddFood.doClick());

        DefaultTableModel foodCartModel = new DefaultTableModel(new String[]{"Menu", "Jumlah", "Total"}, 0);
        JTable foodCartTable = createModernTable(foodCartModel); JScrollPane scrollFoodCart = new JScrollPane(foodCartTable);

        JLabel lblFoodTotal = createCenteredLabel("Total: Rp 0", "TITLE", 24, Font.BOLD);
        PremiumButton btnOrderFood = new PremiumButton("Pesan Sekarang", PremiumButton.Style.PRIMARY); btnOrderFood.setPreferredSize(new Dimension(200, 45));

        fCard.add(lblFoodTitle); fCard.add(Box.createVerticalStrut(30));
        fCard.add(createCenteredLabel("Pilih PC Lo yang lagi Aktif", "MUTED", 14, Font.BOLD)); fCard.add(Box.createVerticalStrut(10));
        fCard.add(cbActivePc); fCard.add(Box.createVerticalStrut(20));

        JPanel foodInputRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); foodInputRow.setOpaque(false);
        cbFoodMenu.setPreferredSize(new Dimension(250, 45)); foodInputRow.add(cbFoodMenu); foodInputRow.add(txtFoodQty); foodInputRow.add(btnAddFood); foodInputRow.add(btnRemoveFood);
        fCard.add(foodInputRow); fCard.add(Box.createVerticalStrut(30));

        fCard.add(scrollFoodCart); fCard.add(Box.createVerticalStrut(20));
        fCard.add(lblFoodTotal); fCard.add(Box.createVerticalStrut(20));

        JPanel foodBtnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); foodBtnWrapper.setOpaque(false); foodBtnWrapper.add(btnOrderFood);
        fCard.add(foodBtnWrapper);

        foodPanel.add(fCard, BorderLayout.CENTER);
        userContentPanel.add(foodPanel, "FOOD");

        // NAVIGATION LOGIC
        btnNavBook.addActionListener(e -> { userContentLayout.show(userContentPanel, "BOOK"); applyGlobalTheme(userContentPanel); });
        btnNavSched.addActionListener(e -> { userContentLayout.show(userContentPanel, "SCHED"); applyGlobalTheme(userContentPanel); });
        btnNavAct.addActionListener(e -> { userContentLayout.show(userContentPanel, "ACT"); applyGlobalTheme(userContentPanel); });
        
        btnNavFood.addActionListener(e -> {
            cbActivePc.removeAllItems();
            try(Connection conn = Database.connect()) {
                // FIX: Menampilkan F&B hanya untuk PC yang beneran lagi AKTIF dimainin
                PreparedStatement ps = conn.prepareStatement("SELECT id, pc_id, pc_type FROM transactions WHERE customer_name = ? AND status = 'AKTIF' AND is_activated = 1");
                ps.setString(1, currentUser);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) { cbActivePc.addItem(new ActivePCItem(rs.getInt("id"), rs.getInt("pc_id"), rs.getString("pc_type"))); }
            } catch(SQLException ex){}
            
            if(cbActivePc.getItemCount() == 0) {
                cbActivePc.setEnabled(false); btnAddFood.setEnabled(false); btnOrderFood.setEnabled(false); btnRemoveFood.setEnabled(false);
                JOptionPane.showMessageDialog(this, "Lo harus punya PC yang lagi nyala/aktif buat pesen ke meja!");
            } else {
                cbActivePc.setEnabled(true); btnAddFood.setEnabled(true); btnOrderFood.setEnabled(true); btnRemoveFood.setEnabled(true);
            }
            cbActivePc.repaint(); btnAddFood.repaint(); btnOrderFood.repaint(); 
            userContentLayout.show(userContentPanel, "FOOD"); applyGlobalTheme(userContentPanel);
        });

        // LOGIKA BUTTONS (F&B)
        btnAddFood.addActionListener(e -> {
            if(menuList.isEmpty()) return; MenuItem selectedItem = menuList.get(cbFoodMenu.getSelectedIndex());
            try {
                int qty = Integer.parseInt(txtFoodQty.getText().trim()); if(qty <= 0) throw new NumberFormatException();
                OrderItem order = new OrderItem(selectedItem, qty); activeFoodCart.add(order); foodCartModel.addRow(new Object[]{selectedItem.name, qty, idrFormat.format(order.hitungTotal())});
                double tot = 0; for(OrderItem item : activeFoodCart) tot += item.hitungTotal();
                lblFoodTotal.setText("Total: " + idrFormat.format(tot)); txtFoodQty.setText("1");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Jumlah ngga valid."); }
        });

        btnRemoveFood.addActionListener(e -> {
            int row = foodCartTable.getSelectedRow();
            if (row >= 0) {
                activeFoodCart.remove(row); foodCartModel.removeRow(row);
                double tot = 0; for(OrderItem item : activeFoodCart) tot += item.hitungTotal();
                lblFoodTotal.setText("Total: " + idrFormat.format(tot));
            } else {
                JOptionPane.showMessageDialog(this, "Pilih dulu pesanan di tabel yang mau dihapus!");
            }
        });

        btnOrderFood.addActionListener(e -> {
            ActivePCItem selectedPc = (ActivePCItem) cbActivePc.getSelectedItem();
            if(selectedPc == null) { JOptionPane.showMessageDialog(this, "Ga ada PC aktif yang dipilih!"); return; }
            if(activeFoodCart.isEmpty()) { JOptionPane.showMessageDialog(this, "Pesanan masih kosong bos!"); return; }

            try (Connection conn = Database.connect()) {
                PreparedStatement pstOrder = conn.prepareStatement("INSERT INTO orders (transaction_id, customer_name, item_name, qty, subtotal, status) VALUES (?, ?, ?, ?, ?, 'PENDING')");
                for (OrderItem item : activeFoodCart) {
                    pstOrder.setInt(1, selectedPc.transId); pstOrder.setString(2, currentUser); pstOrder.setString(3, item.item.name); pstOrder.setInt(4, item.quantity); pstOrder.setDouble(5, item.hitungTotal());
                    pstOrder.addBatch();
                }
                pstOrder.executeBatch();
                
                JOptionPane.showMessageDialog(this, "Pesanan sukses! Tungguin di PC " + selectedPc.pcId + " ntar dianter kasir.");
                activeFoodCart.clear(); foodCartModel.setRowCount(0); lblFoodTotal.setText("Total: Rp 0");
            } catch (SQLException ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Gagal mesen: " + ex.getMessage()); }
        });

        // LOGIKA BUTTONS (BOOKING)
        btnNext.addActionListener(e -> {
            String mod = (String) cbMode.getSelectedItem();
            if(mod == null) return;
            boolean isPlayNow = "Aktifin Sekarang".equals(mod); boolean isJadwal = "Booking dulu".equals(mod); boolean isToken = "Beli Token".equals(mod); boolean isSimpan = "Pakai Sisa Waktu".equals(mod);
            if ((isPlayNow || isJadwal || isSimpan) && selectedPcIdForUser == -1) { JOptionPane.showMessageDialog(this, "Pilih PC dari denah bos."); return; }
            
            int parsedInput = 1;
            try { parsedInput = Integer.parseInt(txtDurasi.getText()); if(parsedInput <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Angka durasi ngga valid."); return; }

            int idx = cbPaket.getSelectedIndex(); 
            String theType = paketPc[idx].split(" -")[0];

            if (isSimpan) {
                int maxSisa = theType.equals("Reguler") ? savedReguler : (theType.equals("VIP") ? savedVip : savedVvip);
                if (parsedInput > maxSisa) { JOptionPane.showMessageDialog(this, "Waktu lu ga cukup boss untuk kelas " + theType + "!\nSisa: " + maxSisa + " menit."); return; }
            }

            if (isJadwal) {
                if(currentBookingDateTime == null) { JOptionPane.showMessageDialog(this, "Pilih tanggal/jam dulu di kalender bos!"); return; }
                if (currentBookingDateTime.isBefore(LocalDateTime.now())) { JOptionPane.showMessageDialog(this, "Pilih waktu masa depan dong, gabisa *time travel*!"); return; }
                try (Connection conn = Database.connect()) {
                    // FIX: Cek jadwal bentrok bener-bener akurat
                    PreparedStatement psCheck = conn.prepareStatement("SELECT id FROM transactions WHERE pc_id = ? AND status IN ('BELUM_DIPAKAI', 'AKTIF', 'DIPESAN') AND booking_datetime IS NOT NULL AND ABS(TIMESTAMPDIFF(MINUTE, booking_datetime, ?)) < 60"); 
                    psCheck.setInt(1, selectedPcIdForUser); psCheck.setTimestamp(2, java.sql.Timestamp.valueOf(currentBookingDateTime));
                    if(psCheck.executeQuery().next()) { JOptionPane.showMessageDialog(this, "Jadwal bentrok sama booking orang lain. Geser dikit jamnya bos."); return; }
                    
                    PreparedStatement psCheckActive = conn.prepareStatement("SELECT id FROM transactions WHERE pc_id = ? AND status = 'AKTIF' AND activated_at IS NOT NULL AND DATE_ADD(activated_at, INTERVAL duration_minutes MINUTE) > ?");
                    psCheckActive.setInt(1, selectedPcIdForUser); psCheckActive.setTimestamp(2, java.sql.Timestamp.valueOf(currentBookingDateTime));
                    if(psCheckActive.executeQuery().next()) { JOptionPane.showMessageDialog(this, "Waktu segitu PC-nya masih dipake orang boss, geser jamnya!"); return; }
                } catch (SQLException ex) {}
            } else { currentBookingDateTime = null; }

            inputFbRow.setVisible(isPlayNow || isSimpan); scrollCart.setVisible(isPlayNow || isSimpan);
            int finalPcId = (isToken) ? 0 : selectedPcIdForUser; 
            int finalDurationMinutes = (isSimpan) ? parsedInput : (parsedInput * 60);
            double finalPrice = (isSimpan) ? 0.0 : hargaPc[idx]; 
            
            currentBooking = new BookingPC(currentUser, theType, finalDurationMinutes, finalPrice, finalPcId, isPlayNow || isSimpan);
            updateTotalPayment(lblTotal); bookFlowLayout.show(bookContentPanel, "STEP2");
        });

        btnBack.addActionListener(e -> bookFlowLayout.show(bookContentPanel, "STEP1"));

        btnAddCart.addActionListener(e -> {
            if(menuList.isEmpty()) return; MenuItem selectedItem = menuList.get(cbMenu.getSelectedIndex());
            try {
                int qty = Integer.parseInt(txtQty.getText().trim()); if(qty <= 0) throw new NumberFormatException();
                OrderItem order = new OrderItem(selectedItem, qty); currentCart.add(order); cartModel.addRow(new Object[]{selectedItem.name, qty, idrFormat.format(order.hitungTotal())});
                updateTotalPayment(lblTotal); txtQty.setText("1");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Jumlah ngga valid."); }
        });

        btnRemoveCart.addActionListener(e -> {
            int row = cartTable.getSelectedRow();
            if (row >= 0) {
                currentCart.remove(row); cartModel.removeRow(row); updateTotalPayment(lblTotal);
            } else {
                JOptionPane.showMessageDialog(this, "Pilih dulu pesanan di tabel yang mau dihapus!");
            }
        });

        btnVerify.addActionListener(e -> {
            String code = txtCode.getText().trim().replace("Kode:", "").replace("Kode: ", "").trim(); if(code.isEmpty()) return;
            try (Connection conn = Database.connect()) {
                PreparedStatement ps = conn.prepareStatement("SELECT id, pc_type, pc_id, status, is_activated, duration_minutes FROM transactions WHERE activation_code = ? AND is_activated = 0 AND status IN ('BELUM_DIPAKAI', 'AKTIF')");
                ps.setString(1, code); ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    verifiedTransId = rs.getInt("id"); verifiedPcType = rs.getString("pc_type"); verifiedPcIdForAct = rs.getInt("pc_id");
                    if (verifiedPcIdForAct > 0) lblVerifyStatus.setText("Kode Valid! Aktifin di PC " + verifiedPcIdForAct);
                    else lblVerifyStatus.setText("Kode Valid! Pilih PC " + verifiedPcType + " bebas di bawah.");
                    lblVerifyStatus.setForeground(Theme.pcActive());
                } else {
                    verifiedTransId = -1; verifiedPcType = ""; verifiedPcIdForAct = -1;
                    lblVerifyStatus.setText("Kode salah atau udah kepake."); lblVerifyStatus.setForeground(Theme.btnDanger());
                }
            } catch (SQLException ex) {}
        });

        btnConfirm.addActionListener(e -> {
            String payMethod = rbTunaiPay.isSelected() ? "Tunai" : "QRIS";
            double pcTotal = currentBooking.hitungTotal(); 
            double foodTotal = 0; int totalSnackQty = 0;
            
            for(OrderItem item : currentCart) { foodTotal += item.hitungTotal(); totalSnackQty += item.quantity; }
            if (totalSnackQty >= 3 && (pcTotal + foodTotal) > 0) { 
                pcTotal = currentBooking.hitungTotal(10.0); 
                JOptionPane.showMessageDialog(this, "Diskon PC 10% applied!"); 
            }

            double grandTotal = pcTotal + foodTotal;
            if(payMethod.equals("QRIS") && grandTotal > 0) { showQRISDialog(grandTotal); }

            String actCode;
            if (currentBooking.getIsPlayNow()) {
                actCode = "LANGSUNG";
            } else {
                String rolePrefix = currentUserRole.equals("MEMBER") ? "MBR" : "USR";
                String typePrefix = currentBooking.getPcType().equals("Reguler") ? "R" : (currentBooking.getPcType().equals("VIP") ? "V" : "VV");
                String kindPrefix = (currentBookingDateTime == null) ? "TKN" : "BKG";
                actCode = kindPrefix + "-" + rolePrefix + "-" + typePrefix + "-" + (int)(Math.random() * 90000 + 10000);
            }

            java.sql.Timestamp sqlBookingDateTime = (currentBookingDateTime != null) ? java.sql.Timestamp.valueOf(currentBookingDateTime) : null;

            try (Connection conn = Database.connect()) {
                if (conn == null) return;
                
                String mod = (String) cbMode.getSelectedItem();
                boolean isSimpanMode = "Pakai Sisa Waktu".equals(mod);
                
                if (isSimpanMode) {
                    String typeCol = currentBooking.getPcType().equals("VVIP") ? "saved_time_vvip" : (currentBooking.getPcType().equals("VIP") ? "saved_time_vip" : "saved_time_reguler");
                    PreparedStatement psUpdateUser = conn.prepareStatement("UPDATE users SET " + typeCol + " = " + typeCol + " - ? WHERE username = ?");
                    psUpdateUser.setInt(1, currentBooking.getDurationMinutes()); psUpdateUser.setString(2, currentUser); psUpdateUser.executeUpdate();
                }

                String sqlTrans = currentBooking.getIsPlayNow() ?
                        "INSERT INTO transactions (customer_name, pc_type, duration, duration_minutes, total_amount, pc_id, activation_code, status, activated_at, payment_method, booking_datetime, is_activated) VALUES (?, ?, ?, ?, ?, ?, ?, 'AKTIF', CURRENT_TIMESTAMP, ?, ?, 1)" :
                        "INSERT INTO transactions (customer_name, pc_type, duration, duration_minutes, total_amount, pc_id, activation_code, status, payment_method, booking_datetime, is_activated) VALUES (?, ?, ?, ?, ?, ?, ?, 'BELUM_DIPAKAI', ?, ?, 0)";
                
                PreparedStatement pstTrans = conn.prepareStatement(sqlTrans, Statement.RETURN_GENERATED_KEYS);
                pstTrans.setString(1, currentUser); pstTrans.setString(2, currentBooking.getPcType()); 
                pstTrans.setInt(3, currentBooking.getDurationMinutes() / 60); pstTrans.setInt(4, currentBooking.getDurationMinutes());      
                pstTrans.setDouble(5, pcTotal); 
                pstTrans.setInt(6, currentBooking.getPcId()); pstTrans.setString(7, actCode); 
                
                if (currentBooking.getIsPlayNow()) {
                    pstTrans.setString(8, payMethod); 
                    if(sqlBookingDateTime != null) pstTrans.setTimestamp(9, sqlBookingDateTime); else pstTrans.setNull(9, java.sql.Types.TIMESTAMP);
                } else {
                    pstTrans.setString(8, payMethod); 
                    if(sqlBookingDateTime != null) pstTrans.setTimestamp(9, sqlBookingDateTime); else pstTrans.setNull(9, java.sql.Types.TIMESTAMP);
                }
                pstTrans.executeUpdate();
                ResultSet rs = pstTrans.getGeneratedKeys(); int transId = 0; if (rs.next()) transId = rs.getInt(1);

                if (!currentCart.isEmpty() && transId > 0) {
                    PreparedStatement pstOrder = conn.prepareStatement("INSERT INTO orders (transaction_id, customer_name, item_name, qty, subtotal, status) VALUES (?, ?, ?, ?, ?, 'PENDING')");
                    for (OrderItem item : currentCart) { pstOrder.setInt(1, transId); pstOrder.setString(2, currentUser); pstOrder.setString(3, item.item.name); pstOrder.setInt(4, item.quantity); pstOrder.setDouble(5, item.hitungTotal()); pstOrder.addBatch(); }
                    pstOrder.executeBatch();
                }

                if(currentBooking.getIsPlayNow()) {
                    PreparedStatement psUpdatePC = conn.prepareStatement("UPDATE pcs SET status = 'AKTIF' WHERE id = ?"); psUpdatePC.setInt(1, currentBooking.getPcId()); psUpdatePC.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Lunas! PC " + currentBooking.getPcId() + " nyala.");
                } else {
                    JOptionPane.showMessageDialog(this, "BERHASIL!\nKode Lo: " + actCode);
                    btnNavSched.setSelected(true); userContentLayout.show(userContentPanel, "SCHED"); applyGlobalTheme(userContentPanel);
                }
            } catch (SQLException ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Error DB: " + ex.getMessage(), "Waduh Error", JOptionPane.ERROR_MESSAGE); return; }

            currentBooking = null; currentCart.clear(); cartModel.setRowCount(0); selectedPcIdForUser = -1; currentBookingDateTime = null;
            for(PCButton b : userBookPcButtons) b.setBorder(null); cbMode.setSelectedIndex(0); cbPaket.setEnabled(false); lblSelectedPc.setText("Pilih dulu PC..."); txtDurasi.setText("1"); txtQty.setText("1"); rbTunaiPay.setSelected(true);
            btnShowCalendar.setText("Pilih Waktu Main..."); btnShowCalendar.setForeground(Theme.text());
            bookFlowLayout.show(bookContentPanel, "STEP1"); fetchPcStatuses(userBookPcButtons, userActivatePcButtons); refreshSchedule(); fetchUserSavedTime();
        });

        userPanel.add(sidebarPanel, BorderLayout.WEST); userPanel.add(userContentPanel, BorderLayout.CENTER);
        startDataSyncTimer(null, null, userBookPcButtons, userActivatePcButtons); refreshSchedule();
    }

    private void refreshSchedule() {
        if(scheduleListPanel == null) return;
        scheduleListPanel.removeAll();
        try (Connection conn = Database.connect()) {
            if (conn == null) return;
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM transactions WHERE customer_name = ? AND (status = 'BELUM_DIPAKAI' OR (status='AKTIF' AND is_activated=1) OR (status='AKTIF' AND is_activated=0)) ORDER BY booking_datetime ASC");
            ps.setString(1, currentUser); ResultSet rs = ps.executeQuery();
            boolean hasData = false;
            while(rs.next()) {
                hasData = true;
                java.sql.Timestamp ts = rs.getTimestamp("booking_datetime");
                java.sql.Timestamp actAt = rs.getTimestamp("activated_at");
                String dbStatus = rs.getString("status");
                String actCodeRaw = rs.getString("activation_code");
                
                // FIX: Menangani tampilan Gas Sekarang agar tulisan menjadi "SEDANG MAIN"
                boolean isGasSekarang = "LANGSUNG".equals(actCodeRaw) || (dbStatus.equals("AKTIF") && rs.getInt("is_activated") == 1);
                String bigText, smallText;
                if (isGasSekarang) { bigText = "SEDANG"; smallText = "MAIN"; } 
                else if (ts == null) { bigText = "TOKEN"; smallText = "Kapan Aja"; } 
                else { bigText = new SimpleDateFormat("dd/MM").format(ts); smallText = new SimpleDateFormat("HH:mm").format(ts); }
                
                String titleDesc = (isGasSekarang || ts != null) ? "PC " + rs.getInt("pc_id") + " (" + rs.getString("pc_type") + ")" : "Token " + rs.getString("pc_type");
                
                int durMin = rs.getInt("duration_minutes"); if(durMin == 0) durMin = rs.getInt("duration") * 60; 

                JPanel scCard = new JPanel(new BorderLayout(20, 0)); scCard.setOpaque(false); scCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, Theme.border()), new EmptyBorder(15, 10, 15, 10)));
                scCard.setMaximumSize(new Dimension(800, 90));

                JPanel datePanel = new JPanel(new GridLayout(2, 1)); datePanel.setOpaque(false); datePanel.setPreferredSize(new Dimension(80, 50));
                
                JLabel lblB = createCenteredLabel(bigText, "TITLE", 20, Font.BOLD);
                if (isGasSekarang) lblB.setForeground(Theme.pcActive());
                datePanel.add(lblB); datePanel.add(createCenteredLabel(smallText, "MUTED", 14, Font.PLAIN));

                JPanel infoPanel = new JPanel(new GridLayout(2, 1)); infoPanel.setOpaque(false);
                JLabel lblPc = new JLabel(titleDesc); lblPc.setFont(Theme.getFont(Font.BOLD, 16)); lblPc.setForeground(Theme.text());
                JLabel lblDur = new JLabel(durMin + " Menit"); lblDur.setFont(Theme.getFont(Font.PLAIN, 14)); lblDur.setForeground(Theme.textMuted());
                infoPanel.add(lblPc); infoPanel.add(lblDur);

                JPanel actionCol = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); actionCol.setOpaque(false);
                if (dbStatus.equals("AKTIF") && rs.getInt("is_activated") == 1) {
                    if (currentUserRole.equals("MEMBER") && actAt != null) {
                        PremiumButton btnSimpan = new PremiumButton("Simpan Waktu", PremiumButton.Style.PRIMARY);
                        int fTransId = rs.getInt("id"); int fPcId = rs.getInt("pc_id"); String fPcType = rs.getString("pc_type");
                        LocalDateTime finalEndT = actAt.toLocalDateTime().plusMinutes(durMin);
                        
                        btnSimpan.addActionListener(e -> {
                            long sisa = Duration.between(LocalDateTime.now(), finalEndT).toMinutes();
                            if(sisa > 0) {
                                try (Connection conn2 = Database.connect()) {
                                    String typeCol = fPcType.equals("VVIP") ? "saved_time_vvip" : (fPcType.equals("VIP") ? "saved_time_vip" : "saved_time_reguler");
                                    conn2.createStatement().executeUpdate("UPDATE users SET " + typeCol + " = " + typeCol + " + " + sisa + " WHERE username = '" + currentUser + "'");
                                    conn2.createStatement().executeUpdate("UPDATE transactions SET status = 'SELESAI' WHERE id = " + fTransId);
                                    conn2.createStatement().executeUpdate("UPDATE pcs SET status = 'TERSEDIA' WHERE id = " + fPcId);
                                    JOptionPane.showMessageDialog(this, "Berhasil simpan " + sisa + " menit ke tabungan kelas " + fPcType + "!");
                                    fetchPcStatuses(userBookPcButtons, userActivatePcButtons); fetchUserSavedTime(); refreshSchedule();
                                } catch (Exception ex) {}
                            } else { JOptionPane.showMessageDialog(this, "Waktu lu udah abis duluan boss!"); }
                        });
                        actionCol.add(btnSimpan);
                    } else {
                        JLabel lblCode = new JLabel("Lagi Dipake"); lblCode.setFont(Theme.getFont(Font.BOLD, 15)); lblCode.setForeground(Theme.pcActive()); actionCol.add(lblCode);
                    }
                } else {
                    JLabel lblCode = new JLabel("Kode: " + actCodeRaw); lblCode.setFont(Theme.getFont(Font.BOLD, 15)); lblCode.setForeground(Theme.pcBooked());
                    actionCol.add(lblCode);
                }
                
                scCard.add(datePanel, BorderLayout.WEST); scCard.add(infoPanel, BorderLayout.CENTER); scCard.add(actionCol, BorderLayout.EAST);
                scheduleListPanel.add(scCard); 
            }
            if(!hasData) { scheduleListPanel.add(createCenteredLabel("Belum ada jadwal/token aktif.", "MUTED", 15, Font.PLAIN)); }
        } catch (SQLException e) {}
        scheduleListPanel.revalidate(); scheduleListPanel.repaint(); applyGlobalTheme(scheduleListPanel);
    }

    // ==========================================
    // UI ADMIN
    // ==========================================
    public void buildAdminUI() {
        adminPanel.removeAll(); adminPcButtons.clear();

        JPanel sidebarPanel = new JPanel(); sidebarPanel.setPreferredSize(new Dimension(280, getHeight()));
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS)); sidebarPanel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel lblLogo = createImageLabel("logo.png", 60, 60, "LOGO");
        JLabel lblAdminTitle = createCenteredLabel("Area Admin", "TITLE", 26, Font.BOLD);
        
        MenuSidebarButton btnNavPantau = new MenuSidebarButton("Pantau PC"); btnNavPantau.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); btnNavPantau.setSelected(true);
        MenuSidebarButton btnNavRiwayat = new MenuSidebarButton("Riwayat"); btnNavRiwayat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        ButtonGroup adminNavGroup = new ButtonGroup(); adminNavGroup.add(btnNavPantau); adminNavGroup.add(btnNavRiwayat);

        JLabel lblStatBooking = createCenteredLabel("Transaksi: 0", "MUTED", 14, Font.PLAIN);
        JLabel lblStatIncome = createCenteredLabel("Rp 0", "TITLE", 22, Font.BOLD);

        PremiumButton btnRefresh = new PremiumButton("Refresh Data", PremiumButton.Style.SECONDARY); btnRefresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        PremiumButton btnToggleTheme = new PremiumButton("Ganti Tema", PremiumButton.Style.SECONDARY); btnToggleTheme.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        PremiumButton btnLogout = new PremiumButton("Tutup Shif", PremiumButton.Style.DANGER); btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        sidebarPanel.add(lblLogo); sidebarPanel.add(Box.createVerticalStrut(15));
        sidebarPanel.add(lblAdminTitle); sidebarPanel.add(Box.createVerticalStrut(30)); 
        sidebarPanel.add(btnNavPantau); sidebarPanel.add(Box.createVerticalStrut(5));
        sidebarPanel.add(btnNavRiwayat); sidebarPanel.add(Box.createVerticalStrut(30));
        sidebarPanel.add(lblStatBooking); sidebarPanel.add(Box.createVerticalStrut(5)); sidebarPanel.add(lblStatIncome);
        sidebarPanel.add(Box.createVerticalStrut(50));
        sidebarPanel.add(btnRefresh); sidebarPanel.add(Box.createVerticalStrut(10));
        sidebarPanel.add(btnToggleTheme); sidebarPanel.add(Box.createVerticalGlue());
        sidebarPanel.add(btnLogout);

        CardLayout adminContentLayout = new CardLayout(); JPanel adminContentPanel = new JPanel(adminContentLayout);

        // TAB 1: Pantau PC
        JPanel pcMonitorPanel = new JPanel(new BorderLayout()); pcMonitorPanel.setBorder(new EmptyBorder(40, 40, 40, 40)); pcMonitorPanel.setOpaque(false);
        PremiumCard pcMonitorCard = new PremiumCard(); pcMonitorCard.setLayout(new BorderLayout(0, 15)); pcMonitorCard.setBorder(new EmptyBorder(30,30,30,30));

        JPanel monitorHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); monitorHeader.setOpaque(false);
        monitorHeader.add(createLegendColor("Kosong", Theme.pcAvailable())); monitorHeader.add(createLegendColor("Nyala", Theme.pcActive()));
        monitorHeader.add(createLegendColor("Perbaikan", Theme.pcMaintenance())); monitorHeader.add(createLegendColor("Dipesan", Theme.pcBooked()));
        pcMonitorCard.add(monitorHeader, BorderLayout.NORTH);

        JPanel floorsPanel = new JPanel(); floorsPanel.setLayout(new BoxLayout(floorsPanel, BoxLayout.Y_AXIS)); floorsPanel.setOpaque(false);
        ActionListener adminPcClickListener = e -> changePCStatusDialog((PCButton) e.getSource());
        floorsPanel.add(createFloorPanel("Lantai 1 \u2014 Reguler", 1, 10, "Reguler", adminPcButtons, adminPcClickListener));
        floorsPanel.add(createFloorPanel("Lantai 2 \u2014 VIP", 11, 20, "VIP", adminPcButtons, adminPcClickListener));
        floorsPanel.add(createFloorPanel("Lantai 3 \u2014 VVIP", 21, 30, "VVIP", adminPcButtons, adminPcClickListener));

        pcMonitorCard.add(new JScrollPane(floorsPanel), BorderLayout.CENTER);
        pcMonitorPanel.add(pcMonitorCard, BorderLayout.CENTER);
        adminContentPanel.add(pcMonitorPanel, "PANTAU");

        // TAB 2: Riwayat & Pesanan Makanan 
        JPanel historyPanel = new JPanel(new BorderLayout(0, 20)); historyPanel.setBorder(new EmptyBorder(40, 40, 40, 40)); historyPanel.setOpaque(false);
        
        PremiumCard orderCard = new PremiumCard(); orderCard.setLayout(new BorderLayout(0, 10)); orderCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        orderCard.add(createCenteredLabel("Daftar Pesanan F&B", "TITLE", 18, Font.BOLD), BorderLayout.NORTH);
        
        DefaultTableModel orderModel = new DefaultTableModel(new String[]{"ID", "Meja PC", "Pemesan", "Menu", "Qty", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable orderTable = createModernTable(orderModel);
        
        orderTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String status = (String) table.getModel().getValueAt(row, 5);
                if (!isSelected) {
                    if ("PENDING".equals(status)) { c.setBackground(Theme.warningBg()); c.setForeground(Theme.btnDanger()); } 
                    else { c.setBackground(Theme.cardBg()); c.setForeground(Theme.text()); }
                }
                return c;
            }
        });
        
        orderCard.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        
        JPanel orderActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); orderActionPanel.setOpaque(false);
        PremiumButton btnAntar = new PremiumButton("Tandai Udah Diantar", PremiumButton.Style.PRIMARY);
        orderActionPanel.add(btnAntar);
        orderCard.add(orderActionPanel, BorderLayout.SOUTH);

        PremiumCard transCard = new PremiumCard(); transCard.setLayout(new BorderLayout(0, 10)); transCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        transCard.add(createCenteredLabel("Riwayat Transaksi PC", "TITLE", 18, Font.BOLD), BorderLayout.NORTH);
        DefaultTableModel transModel = new DefaultTableModel(new String[]{"Trx ID", "Waktu", "Member", "Meja/Tipe", "Durasi", "Total", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable transTable = createModernTable(transModel);
        transCard.add(new JScrollPane(transTable), BorderLayout.CENTER);

        JPanel splitHistory = new JPanel(new GridLayout(2, 1, 0, 20)); splitHistory.setOpaque(false);
        splitHistory.add(orderCard); splitHistory.add(transCard);
        historyPanel.add(splitHistory, BorderLayout.CENTER);
        
        adminContentPanel.add(historyPanel, "RIWAYAT");

        Runnable refreshTables = () -> {
            double totalIncome = 0; int totalTrans = 0;
            try (Connection conn = Database.connect()) {
                if (conn == null) return;
                
                ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM transactions ORDER BY created_at DESC");
                int oldTransRow = transTable.getSelectedRow();
                transModel.setRowCount(0);
                while (rs1.next()) { 
                    totalTrans++; totalIncome += rs1.getDouble("total_amount"); 
                    String meja = rs1.getInt("pc_id") > 0 ? "PC " + rs1.getInt("pc_id") : rs1.getString("pc_type");
                    int durMin = rs1.getInt("duration_minutes"); if(durMin==0) durMin = rs1.getInt("duration")*60;
                    transModel.addRow(new Object[]{
                        rs1.getInt("id"), new SimpleDateFormat("dd/MM HH:mm").format(rs1.getTimestamp("created_at")),
                        rs1.getString("customer_name"), meja, durMin + " Mnt", 
                        idrFormat.format(rs1.getDouble("total_amount")), rs1.getString("status")
                    });
                }
                if(oldTransRow >= 0 && oldTransRow < transTable.getRowCount()) transTable.setRowSelectionInterval(oldTransRow, oldTransRow);

                ResultSet rsFood = conn.createStatement().executeQuery("SELECT SUM(subtotal) AS food_tot FROM orders");
                if (rsFood.next()) totalIncome += rsFood.getDouble("food_tot");

                lblStatBooking.setText("Total Transaksi: " + totalTrans); lblStatIncome.setText(idrFormat.format(totalIncome));

                ResultSet rs2 = conn.createStatement().executeQuery(
                    "SELECT o.id, t.pc_id, o.customer_name, o.item_name, o.qty, o.status " +
                    "FROM orders o LEFT JOIN transactions t ON o.transaction_id = t.id " +
                    "ORDER BY FIELD(o.status, 'PENDING', 'DIANTAR'), o.id DESC"
                );
                int oldOrderRow = orderTable.getSelectedRow();
                orderModel.setRowCount(0);
                while (rs2.next()) {
                    String pcNum = rs2.getInt("pc_id") > 0 ? "PC " + rs2.getInt("pc_id") : "Token/Kasir";
                    orderModel.addRow(new Object[]{
                        rs2.getInt("id"), pcNum, rs2.getString("customer_name"), 
                        rs2.getString("item_name"), rs2.getInt("qty"), rs2.getString("status")
                    });
                }
                if(oldOrderRow >= 0 && oldOrderRow < orderTable.getRowCount()) orderTable.setRowSelectionInterval(oldOrderRow, oldOrderRow);

            } catch (SQLException e) {}
        };

        btnAntar.addActionListener(e -> {
            int row = orderTable.getSelectedRow();
            if(row < 0) { JOptionPane.showMessageDialog(this, "Pilih dulu pesanannya di tabel atas boss!"); return; }
            int orderId = Integer.parseInt(orderModel.getValueAt(row, 0).toString());
            try (Connection conn = Database.connect()) {
                conn.createStatement().executeUpdate("UPDATE orders SET status = 'DIANTAR' WHERE id = " + orderId);
                refreshTables.run();
            } catch (SQLException ex) {}
        });

        btnNavPantau.addActionListener(e -> adminContentLayout.show(adminContentPanel, "PANTAU"));
        btnNavRiwayat.addActionListener(e -> adminContentLayout.show(adminContentPanel, "RIWAYAT"));
        btnRefresh.addActionListener(e -> { fetchPcStatuses(adminPcButtons); refreshTables.run(); });
        btnToggleTheme.addActionListener(e -> toggleTheme());
        btnLogout.addActionListener(e -> { stopDataSyncTimer(); currentUser = ""; currentUserRole = ""; cardLayout.show(mainPanel, "LOGIN"); applyGlobalTheme(this); });

        refreshTables.run();
        adminPanel.add(sidebarPanel, BorderLayout.WEST); adminPanel.add(adminContentPanel, BorderLayout.CENTER);
        
        startDataSyncTimer(refreshTables, null, adminPcButtons);
    }

    // ==========================================
    // LOGIKA AUTO-BILLING & BACKGROUND SYNC
    // ==========================================
    @SafeVarargs
    private final void startDataSyncTimer(Runnable refreshTables, JLabel ignored, List<PCButton>... lists) {
        stopDataSyncTimer(); fetchPcStatuses(lists);
        dataSyncTimer = new Timer(1000, e -> {
            LocalDateTime now = LocalDateTime.now(); boolean needsRefresh = false;
            try (Connection conn = Database.connect()) {
                if (conn != null) {
                    int res1 = conn.createStatement().executeUpdate("UPDATE pcs p JOIN transactions t ON p.id = t.pc_id SET p.status = 'DIPESAN' WHERE t.status = 'BELUM_DIPAKAI' AND t.booking_datetime IS NOT NULL AND t.booking_datetime BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 15 MINUTE) AND p.status = 'TERSEDIA'");
                    if (res1 > 0) needsRefresh = true;
                    int res2 = conn.createStatement().executeUpdate("UPDATE transactions t JOIN pcs p ON p.id = t.pc_id SET t.status = 'AKTIF', t.activated_at = t.booking_datetime, p.status = 'DIPESAN' WHERE t.status = 'BELUM_DIPAKAI' AND t.booking_datetime IS NOT NULL AND t.booking_datetime <= NOW()");
                    if (res2 > 0) needsRefresh = true;
                    ResultSet rsExp = conn.createStatement().executeQuery("SELECT t.id, t.pc_id FROM transactions t WHERE t.status = 'AKTIF' AND t.is_activated = 0 AND t.booking_datetime IS NOT NULL AND t.booking_datetime <= DATE_SUB(NOW(), INTERVAL 30 MINUTE)");
                    while (rsExp.next()) {
                        conn.createStatement().executeUpdate("UPDATE transactions SET status = 'EXPIRED' WHERE id = " + rsExp.getInt("id"));
                        if(rsExp.getInt("pc_id") > 0) conn.createStatement().executeUpdate("UPDATE pcs SET status = 'TERSEDIA' WHERE id = " + rsExp.getInt("pc_id"));
                        needsRefresh = true;
                    }
                }
            } catch (SQLException ex) {}

            for (List<PCButton> list : lists) {
                if (list != null) {
                    for (PCButton btn : list) {
                        if (btn.status != null && btn.status.equals("AKTIF") && btn.endTime != null) {
                            Duration diff = Duration.between(now, btn.endTime);
                            if (!diff.isNegative() && !diff.isZero()) {
                                long ts = diff.getSeconds(); btn.setCountdown(String.format("%02d:%02d:%02d", ts / 3600, (ts % 3600) / 60, ts % 60));
                            } else {
                                btn.setCountdown(""); btn.setEndTime(null); btn.setStatus("TERSEDIA"); 
                                try (Connection conn = Database.connect()) {
                                    conn.createStatement().executeUpdate("UPDATE transactions SET status = 'SELESAI' WHERE pc_id = " + btn.pcId + " AND status = 'AKTIF'");
                                    conn.createStatement().executeUpdate("UPDATE pcs SET status = 'TERSEDIA' WHERE id = " + btn.pcId);
                                    needsRefresh = true;
                                } catch (Exception ex) {}
                            }
                        } else if (btn.status != null && btn.status.equals("DIPESAN")) { btn.setCountdown(""); } 
                        else { btn.setCountdown(""); }
                    }
                }
            }
            if (needsRefresh) { fetchPcStatuses(lists); if (refreshTables != null) refreshTables.run(); refreshSchedule(); }
        });
        dataSyncTimer.start();
    }

    private void stopDataSyncTimer() { if (dataSyncTimer != null) dataSyncTimer.stop(); }

    @SafeVarargs
    private final void fetchPcStatuses(List<PCButton>... lists) {
        try (Connection conn = Database.connect()) {
            if (conn == null) return;
            String query = "SELECT p.id, p.status, t.activated_at, t.duration, t.duration_minutes, t.is_activated, t.status AS trans_stat FROM pcs p LEFT JOIN transactions t ON p.id = t.pc_id AND t.status = 'AKTIF'";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while(rs.next()) {
                int id = rs.getInt("id"); String status = rs.getString("status"); LocalDateTime endTime = null;
                if (rs.getString("trans_stat") != null && rs.getString("trans_stat").equals("AKTIF") && rs.getTimestamp("activated_at") != null) {
                    int durMin = rs.getInt("duration_minutes");
                    if (durMin == 0) durMin = rs.getInt("duration") * 60; 
                    endTime = rs.getTimestamp("activated_at").toLocalDateTime().plusMinutes(durMin);
                }
                for (List<PCButton> list : lists) {
                    if (list != null && id >= 1 && id <= list.size()) { list.get(id - 1).setStatus(status); list.get(id - 1).setEndTime(endTime); }
                }
            }
        } catch (SQLException e) {}
    }

    private void changePCStatusDialog(PCButton btn) {
        String[] options = {"TERSEDIA", "AKTIF", "PERBAIKAN", "DIPESAN"};
        String selected = (String) JOptionPane.showInputDialog(this, "Status PC " + btn.pcId + ":", "Pengaturan", JOptionPane.PLAIN_MESSAGE, null, options, btn.status);
        if (selected != null && !selected.equals(btn.status)) {
            try (Connection conn = Database.connect()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE pcs SET status = ? WHERE id = ?"); ps.setString(1, selected); ps.setInt(2, btn.pcId); ps.executeUpdate(); 
                if (!selected.equals("AKTIF")) { conn.createStatement().executeUpdate("UPDATE transactions SET status = 'SELESAI' WHERE pc_id = " + btn.pcId + " AND status = 'AKTIF'"); btn.setEndTime(null); btn.setCountdown(""); }
                btn.setStatus(selected);
            } catch (SQLException ex) {}
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================
    private JPanel createFloorPanel(String title, int startId, int endId, String type, List<PCButton> list, ActionListener listener) {
        JPanel panel = new JPanel(new BorderLayout(0, 15)); panel.setOpaque(false); panel.setBorder(new EmptyBorder(10, 0, 25, 0));
        panel.add(createCenteredLabel(title, "TITLE", 17, Font.BOLD), BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(2, 5, 12, 12)); grid.setOpaque(false);
        for(int i = startId; i <= endId; i++) { PCButton btn = new PCButton(i, type); btn.addActionListener(listener); list.add(btn); grid.add(btn); }
        panel.add(grid, BorderLayout.CENTER); return panel;
    }

    private JPanel createLegendColor(String text, Color c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)); p.setOpaque(false);
        JPanel box = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c); g2.fillOval(0, 0, 12, 12); super.paintComponent(g); g2.dispose();
            }
        }; box.setOpaque(false); box.setPreferredSize(new Dimension(12, 14));
        JLabel l = new JLabel(text); l.setName("TITLE"); l.setFont(Theme.getFont(Font.PLAIN, 13)); p.add(box); p.add(l); return p;
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(Theme.getFont(Font.PLAIN, 15)); 
        cb.setMaximumSize(new Dimension(340, 45)); cb.setAlignmentX(Component.CENTER_ALIGNMENT);
        ((JLabel)cb.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        cb.setUI(new BasicComboBoxUI() { 
            @Override protected JButton createArrowButton() { 
                JButton btn = new JButton() {
                    @Override public void paintComponent(Graphics g) {
                        super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Theme.textMuted()); int w = getWidth(), h = getHeight();
                        int[] x = {w/2 - 5, w/2 + 5, w/2}; int[] y = {h/2 - 2, h/2 - 2, h/2 + 3};
                        g2.fillPolygon(x, y, 3);
                    }
                };
                btn.setOpaque(false); btn.setBorder(BorderFactory.createEmptyBorder()); btn.setFocusPainted(false);
                return btn; 
            } 
        });
    }

    private JTable createModernTable(DefaultTableModel model) {
        JTable table = new JTable(model); table.setRowHeight(40); table.setFont(Theme.getFont(Font.PLAIN, 15));
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Theme.btnPrimary()); table.setSelectionForeground(Color.WHITE);
        JTableHeader header = table.getTableHeader(); header.setFont(Theme.getFont(Font.BOLD, 14)); header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border())); header.setPreferredSize(new Dimension(100, 45));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        ((DefaultTableCellRenderer)table.getDefaultRenderer(Object.class)).setHorizontalAlignment(JLabel.CENTER);
        return table;
    }

    private void updateTotalPayment(JLabel lblTotal) {
        double total = 0; if(currentBooking != null) total += currentBooking.hitungTotal();
        for(OrderItem item : currentCart) total += item.hitungTotal();
        lblTotal.setText("Total: " + idrFormat.format(total));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> { Warnet app = new Warnet(); app.setVisible(true); });
    }
}