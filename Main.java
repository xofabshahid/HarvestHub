import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JFrame {

    // ============================================================
    // BRAND / UI
    // ============================================================

    private static final Color BG = new Color(246, 248, 245);
    private static final Color SURFACE = Color.WHITE;
    private static final Color FOREST = new Color(35, 92, 62);
    private static final Color FOREST_DARK = new Color(25, 65, 44);
    private static final Color SAGE = new Color(228, 239, 231);
    private static final Color SAGE_LIGHT = new Color(241, 247, 242);
    private static final Color TEXT = new Color(30, 39, 34);
    private static final Color MUTED = new Color(104, 116, 109);
    private static final Color BORDER = new Color(222, 228, 223);
    private static final Color ACCENT = new Color(224, 154, 78);
    private static final Color RESCUE = new Color(181, 63, 63);
    private static final Color SOFT_RED = new Color(251, 235, 235);
    private static final Color SOFT_ORANGE = new Color(252, 242, 226);
    private static final Color SOFT_GREEN = new Color(232, 244, 236);
    private static final Color BLUE_GRAY = new Color(71, 92, 105);

    // Consistent type scale used throughout the app.
    private static final int FONT_PAGE_TITLE = 28;
    private static final int FONT_HERO_TITLE = 30;
    private static final int FONT_CARD_TITLE = 18;
    private static final int FONT_SUBTITLE = 14;
    private static final int FONT_BODY = 13;
    private static final int FONT_FIELD_LABEL = 12;
    private static final int FONT_EYEBROW = 10;
    private static final int FONT_STAT_VALUE = 24;

    private static final String DATA_FILE = "harvesthub-data.ser";

    private AppData data;
    private User currentUser;

    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootLayout);

    private final CardLayout pageLayout = new CardLayout();
    private JPanel pagePanel;
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    private JPanel marketplaceGrid;
    private JPanel favoritesGrid;
    private JPanel myListingsPanel;
    private JTextField marketplaceSearchField;
    private JComboBox<String> marketplaceFilter;
    private JComboBox<String> marketplaceSort;

    private JLabel impactPoundsLabel;
    private JLabel impactFamiliesLabel;
    private JLabel impactDonationsLabel;
    private JLabel impactListingsLabel;
    private InteractiveMapPanel communityMapPanel;

    // Competition-mode intelligence features
    private WeatherSnapshot currentWeather;
    private JComboBox<String> smartMatchListingBox;
    private final ArrayList<Integer> smartMatchListingIds = new ArrayList<>();
    private JPanel smartMatchResultsPanel;
    private JLabel smartRouteSummaryLabel;
    private JTextArea smartRouteDetailsArea;
    private ProduceListing lastMatchedListing;
    private List<RecipientProfile> lastOptimizedRoute = new ArrayList<>();

    private BufferedImage latestScanImage;
    private String latestScanFileName = "";
    private int latestVisualRiskAdjustment = 0;
    private JLabel scanPreviewLabel;
    private JLabel scanScoreLabel;
    private JProgressBar scanQualityBar;
    private JTextArea scanDetailsArea;

    private JPanel validationListPanel;
    private JLabel validationInterviewLabel;
    private JLabel validationPilotLabel;
    private JLabel validationExpertLabel;
    private JLabel validationBenchmarkLabel;

    public Main() {
        data = loadData();
        ensureDataCompatibility();
        configureSwingDefaults();

        setTitle("HarvestHub");
        setSize(1240, 800);
        setMinimumSize(new Dimension(1040, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });

        add(rootPanel);
        showLoginScreen();
        setVisible(true);
    }

    private void configureSwingDefaults() {
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("CheckBox.background", SURFACE);
        UIManager.put("CheckBox.foreground", TEXT);
        ToolTipManager.sharedInstance().setInitialDelay(250);
    }

    // ============================================================
    // LOGIN / ACCOUNT CREATION
    // ============================================================

    private void showLoginScreen() {
        rootPanel.removeAll();

        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(38, 48, 38, 48));

        ShadowPanel shell = new ShadowPanel(30, SURFACE);
        shell.setLayout(new GridLayout(1, 2));
        shell.setPreferredSize(new Dimension(980, 610));

        JPanel brandSide = new JPanel();
        brandSide.setBackground(FOREST_DARK);
        brandSide.setBorder(new EmptyBorder(52, 46, 48, 46));
        brandSide.setLayout(new BoxLayout(brandSide, BoxLayout.Y_AXIS));

        LogoMark mark = new LogoMark();
        mark.setPreferredSize(new Dimension(58, 58));
        mark.setMaximumSize(new Dimension(58, 58));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brand = new JLabel("HarvestHub");
        brand.setFont(new Font("SansSerif", Font.BOLD, 38));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel kicker = new JLabel("BUILT FOR LOCAL COMMUNITIES");
        kicker.setFont(new Font("SansSerif", Font.BOLD, 11));
        kicker.setForeground(new Color(183, 213, 193));
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea story = new JTextArea(
                "Helping extra harvest find a home before it becomes waste. " +
                        "HarvestHub connects growers around Waller with neighbors who can use what they grow."
        );
        story.setFont(new Font("SansSerif", Font.PLAIN, 18));
        story.setForeground(new Color(235, 244, 238));
        story.setOpaque(false);
        story.setEditable(false);
        story.setLineWrap(true);
        story.setWrapStyleWord(true);
        story.setAlignmentX(Component.LEFT_ALIGNMENT);
        story.setMaximumSize(new Dimension(380, 130));

        RoundedPanel mini = new RoundedPanel(20, new Color(255, 255, 255, 22));
        mini.setLayout(new BoxLayout(mini, BoxLayout.Y_AXIS));
        mini.setBorder(new EmptyBorder(18, 20, 18, 20));
        mini.setAlignmentX(Component.LEFT_ALIGNMENT);
        mini.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));

        JLabel miniTitle = new JLabel("From Waller, for Waller");
        miniTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        miniTitle.setForeground(Color.WHITE);
        JLabel miniText = new JLabel("<html>Local produce • less waste • stronger connections</html>");
        miniText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        miniText.setForeground(new Color(218, 233, 223));

        mini.add(miniTitle);
        mini.add(Box.createVerticalStrut(7));
        mini.add(miniText);

        brandSide.add(mark);
        brandSide.add(Box.createVerticalStrut(24));
        brandSide.add(kicker);
        brandSide.add(Box.createVerticalStrut(10));
        brandSide.add(brand);
        brandSide.add(Box.createVerticalStrut(20));
        brandSide.add(story);
        brandSide.add(Box.createVerticalGlue());
        brandSide.add(mini);

        JPanel loginSide = new JPanel();
        loginSide.setBackground(SURFACE);
        loginSide.setBorder(new EmptyBorder(54, 58, 48, 58));
        loginSide.setLayout(new BoxLayout(loginSide, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 30));
        welcome.setForeground(TEXT);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to continue to your local food community.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel motto = new JLabel("Grow local. Waste less. Share more.");
        motto.setFont(new Font("SansSerif", Font.BOLD, 13));
        motto.setForeground(FOREST);
        motto.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField usernameField = styledTextField();
        JPasswordField passwordField = styledPasswordField();

        RoundedButton signIn = new RoundedButton("Sign in", FOREST, Color.WHITE);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton createAccount = new RoundedButton("Create an account", SAGE, FOREST_DARK);
        createAccount.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        createAccount.setAlignmentX(Component.LEFT_ALIGNMENT);

        signIn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            User found = findUser(username);

            if (found == null || !found.password.equals(password)) {
                JOptionPane.showMessageDialog(this,
                        "That username or password does not match.",
                        "Could not sign in",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            currentUser = found;
            showMainApp();
        });
        passwordField.addActionListener(e -> signIn.doClick());
        createAccount.addActionListener(e -> showCreateAccountDialog());

        loginSide.add(welcome);
        loginSide.add(Box.createVerticalStrut(8));
        loginSide.add(subtitle);
        loginSide.add(Box.createVerticalStrut(8));
        loginSide.add(motto);
        loginSide.add(Box.createVerticalStrut(26));
        loginSide.add(labeledField("Username", usernameField));
        loginSide.add(Box.createVerticalStrut(15));
        loginSide.add(labeledField("Password", passwordField));
        loginSide.add(Box.createVerticalStrut(24));
        loginSide.add(signIn);
        loginSide.add(Box.createVerticalStrut(10));
        loginSide.add(createAccount);
        loginSide.add(Box.createVerticalGlue());

        shell.add(brandSide);
        shell.add(loginSide);
        page.add(shell);

        rootPanel.add(page, "LOGIN");
        rootLayout.show(rootPanel, "LOGIN");
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private void showCreateAccountDialog() {
        final int FORM_WIDTH = 440;
        final int FIELD_HEIGHT = 41;

        JTextField nameField = styledTextField();
        JTextField usernameField = styledTextField();
        JPasswordField passwordField = styledPasswordField();

        JComboBox<String> roleBox = styledCombo(new String[]{"Community Member", "Farmer"});
        JTextField farmField = styledTextField();
        JTextField addressField = styledTextField();
        JTextField cityField = styledTextField();
        JTextField zipField = styledTextField();
        cityField.setText("Waller");
        zipField.setText("77484");
        farmField.setEnabled(false);

        // Every single-line control uses the same height. The width is controlled
        // by the centered form column rather than by each component independently.
        JComponent[] fullWidthControls = {
                nameField, usernameField, passwordField, roleBox, farmField, addressField
        };
        for (JComponent control : fullWidthControls) {
            control.setPreferredSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
            control.setMinimumSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
            control.setMaximumSize(new Dimension(FORM_WIDTH, FIELD_HEIGHT));
            control.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        JCheckBox preciseLocationBox = new JCheckBox("Use this location privately for more accurate nearby matches");
        preciseLocationBox.setOpaque(false);
        preciseLocationBox.setSelected(true);
        preciseLocationBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        preciseLocationBox.setForeground(TEXT);
        preciseLocationBox.setFocusPainted(false);
        preciseLocationBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        preciseLocationBox.setPreferredSize(new Dimension(FORM_WIDTH, 28));
        preciseLocationBox.setMaximumSize(new Dimension(FORM_WIDTH, 28));

        JCheckBox defaultPickupBox = new JCheckBox("Use this as my default farm pickup location");
        defaultPickupBox.setOpaque(false);
        defaultPickupBox.setEnabled(false);
        defaultPickupBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        defaultPickupBox.setForeground(TEXT);
        defaultPickupBox.setFocusPainted(false);
        defaultPickupBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        defaultPickupBox.setPreferredSize(new Dimension(FORM_WIDTH, 28));
        defaultPickupBox.setMaximumSize(new Dimension(FORM_WIDTH, 28));

        JLabel addressLabel = new JLabel("Street address (optional for community members)");
        addressLabel.setFont(new Font("SansSerif", Font.BOLD, FONT_FIELD_LABEL));
        addressLabel.setForeground(TEXT);
        addressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel addressGroup = new JPanel();
        addressGroup.setOpaque(false);
        addressGroup.setLayout(new BoxLayout(addressGroup, BoxLayout.Y_AXIS));
        addressGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressGroup.setPreferredSize(new Dimension(FORM_WIDTH, 66));
        addressGroup.setMinimumSize(new Dimension(FORM_WIDTH, 66));
        addressGroup.setMaximumSize(new Dimension(FORM_WIDTH, 66));
        addressGroup.add(addressLabel);
        addressGroup.add(Box.createVerticalStrut(5));
        addressGroup.add(addressField);

        Runnable updateRoleFields = () -> {
            boolean farmer = roleBox.getSelectedIndex() == 1;
            farmField.setEnabled(farmer);
            defaultPickupBox.setEnabled(farmer);
            defaultPickupBox.setSelected(farmer);
            addressLabel.setText(farmer
                    ? "Farm / pickup street address"
                    : "Street address (optional for community members)");
        };
        roleBox.addActionListener(e -> updateRoleFields.run());
        updateRoleFields.run();

        // -----------------------------------------------------------------
        // ONE VISUAL COLUMN
        // -----------------------------------------------------------------
        // All content below shares the exact same 440 px width and left edge.
        // This avoids BoxLayout's mixed-alignment behavior that previously made
        // the fields appear shifted relative to the heading and section labels.
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(SURFACE);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(new EmptyBorder(4, 0, 12, 0));
        form.setPreferredSize(new Dimension(FORM_WIDTH, 720));
        form.setMinimumSize(new Dimension(FORM_WIDTH, 720));
        form.setMaximumSize(new Dimension(FORM_WIDTH, 900));

        JLabel dialogTitle = new JLabel("Create your account");
        dialogTitle.setFont(new Font("SansSerif", Font.BOLD, 27));
        dialogTitle.setForeground(TEXT);
        dialogTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dialogSubtitle = new JLabel("Join HarvestHub as a community member or farmer.");
        dialogSubtitle.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        dialogSubtitle.setForeground(MUTED);
        dialogSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(dialogTitle);
        form.add(Box.createVerticalStrut(6));
        form.add(dialogSubtitle);
        form.add(Box.createVerticalStrut(22));

        JLabel accountEyebrow = new JLabel("ACCOUNT DETAILS");
        accountEyebrow.setFont(new Font("SansSerif", Font.BOLD, FONT_EYEBROW));
        accountEyebrow.setForeground(FOREST);
        accountEyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(accountEyebrow);
        form.add(Box.createVerticalStrut(9));

        form.add(fixedLabeledField("Your name", nameField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(9));
        form.add(fixedLabeledField("Username", usernameField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(9));
        form.add(fixedLabeledField("Password", passwordField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(9));
        form.add(fixedLabeledField("Account type", roleBox, FORM_WIDTH));
        form.add(Box.createVerticalStrut(9));
        form.add(fixedLabeledField("Farm name (farmers only)", farmField, FORM_WIDTH));
        form.add(Box.createVerticalStrut(20));

        JLabel locationEyebrow = new JLabel("LOCATION & PRIVACY");
        locationEyebrow.setFont(new Font("SansSerif", Font.BOLD, FONT_EYEBROW));
        locationEyebrow.setForeground(FOREST);
        locationEyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(locationEyebrow);
        form.add(Box.createVerticalStrut(6));

        JTextArea locationIntro = textArea(
                "HarvestHub uses your location to estimate distance and find nearby matches. " +
                        "Exact addresses stay private and are never displayed publicly.",
                12, MUTED);
        locationIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        locationIntro.setPreferredSize(new Dimension(FORM_WIDTH, 48));
        locationIntro.setMinimumSize(new Dimension(FORM_WIDTH, 48));
        locationIntro.setMaximumSize(new Dimension(FORM_WIDTH, 48));
        form.add(locationIntro);
        form.add(Box.createVerticalStrut(10));
        form.add(addressGroup);
        form.add(Box.createVerticalStrut(9));

        // City and ZIP are the only split row. Each half is identical in width
        // and inherits the same outside edges as the rest of the form.
        JPanel cityZip = new JPanel(new GridLayout(1, 2, 12, 0));
        cityZip.setOpaque(false);
        cityZip.setAlignmentX(Component.LEFT_ALIGNMENT);
        cityZip.setPreferredSize(new Dimension(FORM_WIDTH, 66));
        cityZip.setMinimumSize(new Dimension(FORM_WIDTH, 66));
        cityZip.setMaximumSize(new Dimension(FORM_WIDTH, 66));
        cityZip.add(labeledField("City", cityField));
        cityZip.add(labeledField("ZIP code", zipField));
        form.add(cityZip);
        form.add(Box.createVerticalStrut(10));
        form.add(preciseLocationBox);
        form.add(Box.createVerticalStrut(4));
        form.add(defaultPickupBox);
        form.add(Box.createVerticalStrut(12));

        RoundedPanel privacyCard = new RoundedPanel(16, SAGE_LIGHT);
        privacyCard.setLayout(new BorderLayout());
        privacyCard.setBorder(new EmptyBorder(11, 13, 11, 13));
        privacyCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        privacyCard.setPreferredSize(new Dimension(FORM_WIDTH, 66));
        privacyCard.setMinimumSize(new Dimension(FORM_WIDTH, 66));
        privacyCard.setMaximumSize(new Dimension(FORM_WIDTH, 66));
        JTextArea privacyCopy = textArea(
                "Public view: city + distance only. After a reservation, the pickup address can be shared privately with that recipient.",
                11, FOREST_DARK);
        privacyCard.add(privacyCopy, BorderLayout.CENTER);
        form.add(privacyCard);

        // Center the single fixed-width column in the viewport. The form itself
        // remains left-aligned internally, which makes labels and controls easy
        // to scan and prevents the offset visible in the previous dialog.
        JPanel centeredWrapper = new JPanel(new GridBagLayout());
        centeredWrapper.setBackground(SURFACE);
        centeredWrapper.setBorder(new EmptyBorder(18, 24, 12, 24));
        GridBagConstraints centerGbc = new GridBagConstraints();
        centerGbc.gridx = 0;
        centerGbc.gridy = 0;
        centerGbc.weightx = 1.0;
        centerGbc.weighty = 1.0;
        centerGbc.anchor = GridBagConstraints.NORTH;
        centeredWrapper.add(form, centerGbc);

        JScrollPane formScroll = cleanScroll(centeredWrapper);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(18);
        formScroll.setBorder(BorderFactory.createEmptyBorder());

        JDialog dialog = new JDialog(this, "Create HarvestHub Account", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setMinimumSize(new Dimension(570, 650));
        dialog.setSize(new Dimension(620, 760));
        dialog.setLocationRelativeTo(this);

        JPanel dialogShell = new JPanel(new BorderLayout());
        dialogShell.setBackground(SURFACE);
        dialogShell.add(formScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SURFACE);
        footer.setBorder(new EmptyBorder(12, 28, 18, 28));

        JLabel helper = new JLabel("Your exact address is never shown publicly.");
        helper.setFont(new Font("SansSerif", Font.PLAIN, 11));
        helper.setForeground(MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        RoundedButton cancel = new RoundedButton("Cancel", new Color(241, 243, 241), TEXT);
        RoundedButton create = new RoundedButton("Create account", FOREST, Color.WHITE);
        cancel.setPreferredSize(new Dimension(96, 42));
        create.setPreferredSize(new Dimension(132, 42));
        actions.add(cancel);
        actions.add(create);

        footer.add(helper, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        dialogShell.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(dialogShell);

        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(create);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        create.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            boolean isFarmer = roleBox.getSelectedIndex() == 1;
            String farmName = farmField.getText().trim();
            String streetAddress = addressField.getText().trim();
            String city = cityField.getText().trim();
            String zip = zipField.getText().trim();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please fill in your name, username, and password.",
                        "Missing account details", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isFarmer && farmName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter your farm name.",
                        "Farm name required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (city.isEmpty() || zip.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter your city and ZIP code so HarvestHub can estimate nearby matches.",
                        "Location required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isFarmer && streetAddress.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a farm or pickup street address. HarvestHub keeps it private until a pickup is reserved.",
                        "Pickup address required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (findUser(username) != null) {
                JOptionPane.showMessageDialog(dialog,
                        "That username is already being used.",
                        "Username unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User newUser = new User(data.nextUserId++, name, username, password,
                    isFarmer ? "FARMER" : "COMMUNITY", isFarmer ? farmName : "");
            newUser.streetAddress = streetAddress;
            newUser.city = city;
            newUser.zipCode = zip;
            newUser.usePreciseLocation = preciseLocationBox.isSelected() && !streetAddress.isBlank();
            newUser.defaultPickupLocation = isFarmer && defaultPickupBox.isSelected();

            Coordinate approximate = approximateCoordinateForLocation(city + ", TX " + zip);
            newUser.latitude = approximate.lat;
            newUser.longitude = approximate.lon;
            newUser.locationIsPrecise = false;

            data.users.add(newUser);
            currentUser = newUser;
            saveData();
            dialog.dispose();
            showMainApp();

            if (newUser.usePreciseLocation) {
                geocodeUserLocationInBackground(newUser);
            }
        });

        dialog.setVisible(true);
    }

    private User findUser(String username) {
        for (User user : data.users) {
            if (user.username.equalsIgnoreCase(username)) return user;
        }
        return null;
    }

    private User findUserById(int id) {
        for (User user : data.users) {
            if (user.id == id) return user;
        }
        return null;
    }

    private String publicLocationForUser(User user) {
        if (user == null) return "Waller area";
        String city = user.city == null || user.city.isBlank() ? "Waller" : user.city.trim();
        String zip = user.zipCode == null ? "" : user.zipCode.trim();
        return zip.isEmpty() ? city + ", TX" : city + ", TX " + zip;
    }

    private String privateAddressForUser(User user) {
        if (user == null || user.streetAddress == null || user.streetAddress.isBlank()) return publicLocationForUser(user);
        return user.streetAddress.trim() + ", " + publicLocationForUser(user);
    }

    private String displayLocationForListing(ProduceListing listing) {
        if (listing == null) return "Waller area";
        User owner = findUserById(listing.ownerUserId);
        if (owner != null && owner.isFarmer() && owner.city != null && !owner.city.isBlank()) {
            return publicLocationForUser(owner);
        }
        String raw = listing.location == null ? "" : listing.location.trim();
        if (raw.matches(".*\\d.*") && raw.contains(",")) {
            String lower = raw.toLowerCase();
            if (lower.contains("waller")) return "Waller, TX";
            if (lower.contains("prairie view")) return "Prairie View, TX";
            if (lower.contains("hempstead")) return "Hempstead, TX";
            return "Local pickup area";
        }
        return raw.isBlank() ? "Waller area" : raw;
    }

    private Coordinate coordinateForListing(ProduceListing listing) {
        if (listing == null) return approximateCoordinateForLocation("Waller, TX");
        User owner = findUserById(listing.ownerUserId);
        if (owner != null && owner.latitude != 0 && owner.longitude != 0) {
            String privateAddress = privateAddressForUser(owner);
            String listingLocation = listing.location == null ? "" : listing.location.trim();
            if (listingLocation.equalsIgnoreCase(privateAddress)
                    || listingLocation.equalsIgnoreCase(owner.streetAddress == null ? "" : owner.streetAddress.trim())
                    || listingLocation.equalsIgnoreCase(publicLocationForUser(owner))) {
                return new Coordinate(owner.latitude, owner.longitude);
            }
        }
        return approximateCoordinateForLocation(listing.location);
    }

    private double distanceFromCurrentUser(ProduceListing listing) {
        if (currentUser == null || currentUser.latitude == 0 || currentUser.longitude == 0) return -1;
        Coordinate farm = coordinateForListing(listing);
        return distanceMiles(currentUser.latitude, currentUser.longitude, farm.lat, farm.lon);
    }

    private String marketplaceLocationText(ProduceListing listing) {
        StringBuilder text = new StringBuilder(displayLocationForListing(listing));
        if (currentUser != null && !currentUser.isFarmer()) {
            double miles = distanceFromCurrentUser(listing);
            if (miles >= 0) text.append("  •  ").append(String.format(Locale.US, "%.1f mi away", miles));
        }
        return text.toString();
    }

    private void geocodeUserLocationInBackground(User user) {
        if (user == null || user.streetAddress == null || user.streetAddress.isBlank()) return;
        String query = privateAddressForUser(user);
        SwingWorker<Coordinate, Void> worker = new SwingWorker<>() {
            @Override
            protected Coordinate doInBackground() {
                return requestGeocodeCoordinate(query);
            }

            @Override
            protected void done() {
                try {
                    Coordinate coordinate = get();
                    if (coordinate != null) {
                        user.latitude = coordinate.lat;
                        user.longitude = coordinate.lon;
                        user.locationIsPrecise = true;
                        saveData();
                        refreshMarketplaceCards();
                        if (communityMapPanel != null) communityMapPanel.reloadFarmMarkers();
                    }
                } catch (Exception ignored) {
                    // The app safely keeps the city-level estimate if precise geocoding is unavailable.
                }
            }
        };
        worker.execute();
    }

    private Coordinate requestGeocodeCoordinate(String query) {
        HttpURLConnection connection = null;
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URL url = new URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encoded);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "HarvestHubStudentPrototype/1.0 (Java Swing educational project)");
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) return null;

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }
            Pattern latPattern = Pattern.compile("\\\"lat\\\"\\s*:\\s*\\\"(-?\\d+(?:\\.\\d+)?)\\\"");
            Pattern lonPattern = Pattern.compile("\\\"lon\\\"\\s*:\\s*\\\"(-?\\d+(?:\\.\\d+)?)\\\"");
            Matcher latMatcher = latPattern.matcher(json.toString());
            Matcher lonMatcher = lonPattern.matcher(json.toString());
            if (!latMatcher.find() || !lonMatcher.find()) return null;
            return new Coordinate(Double.parseDouble(latMatcher.group(1)), Double.parseDouble(lonMatcher.group(1)));
        } catch (IOException | NumberFormatException ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    // ============================================================
    // MAIN APP SHELL / NAVIGATION
    // ============================================================

    private void showMainApp() {
        rootPanel.removeAll();
        navButtons.clear();

        JPanel app = new JPanel(new BorderLayout());
        app.setBackground(BG);

        JPanel sidebar = createSidebar();

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.add(createTopBar(), BorderLayout.NORTH);

        pagePanel = new JPanel(pageLayout);
        pagePanel.setBackground(BG);

        pagePanel.add(createHomePanel(), "Home");
        pagePanel.add(createMarketplacePanel(), "Marketplace");
        pagePanel.add(createHarvestHelperPanel(), "Harvest Helper");
        if (currentUser.isFarmer()) {
            pagePanel.add(createSmartMatchPanel(), "Smart Match");
            pagePanel.add(createHarvestScanPanel(), "HarvestScan");
        }
        pagePanel.add(createMapPanel(), "Community Map");

        if (currentUser.isFarmer()) {
            pagePanel.add(createFarmerPanel(), "My Farm");
        } else {
            pagePanel.add(createFavoritesPanel(), "Favorites");
        }

        pagePanel.add(createImpactPanel(), "Impact");

        center.add(pagePanel, BorderLayout.CENTER);
        app.add(sidebar, BorderLayout.WEST);
        app.add(center, BorderLayout.CENTER);

        rootPanel.add(app, "APP");
        rootLayout.show(rootPanel, "APP");
        showPage("Home");
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private JPanel createSidebar() {
        JPanel side = new JPanel();
        side.setBackground(FOREST_DARK);
        side.setPreferredSize(new Dimension(225, 0));
        side.setBorder(new EmptyBorder(24, 16, 22, 16));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JPanel logoRow = new JPanel();
        logoRow.setOpaque(false);
        logoRow.setLayout(new BoxLayout(logoRow, BoxLayout.X_AXIS));
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        LogoMark mark = new LogoMark();
        mark.setPreferredSize(new Dimension(36, 36));
        mark.setMaximumSize(new Dimension(36, 36));
        JLabel logo = new JLabel("HarvestHub");
        logo.setFont(new Font("SansSerif", Font.BOLD, 21));
        logo.setForeground(Color.WHITE);

        logoRow.add(mark);
        logoRow.add(Box.createHorizontalStrut(10));
        logoRow.add(logo);

        JLabel location = new JLabel("WALLER, TEXAS");
        location.setFont(new Font("SansSerif", Font.BOLD, 10));
        location.setForeground(new Color(169, 199, 178));
        location.setAlignmentX(Component.LEFT_ALIGNMENT);

        side.add(logoRow);
        side.add(Box.createVerticalStrut(7));
        side.add(location);
        side.add(Box.createVerticalStrut(30));

        addNavButton(side, "Home", "⌂");
        addNavButton(side, "Marketplace", "◫");
        addNavButton(side, "Harvest Helper", "✦");
        if (currentUser.isFarmer()) {
            addNavButton(side, "Smart Match", "◎");
            addNavButton(side, "HarvestScan", "◉");
        }
        addNavButton(side, "Community Map", "⌖");
        addNavButton(side, currentUser.isFarmer() ? "My Farm" : "Favorites", currentUser.isFarmer() ? "♣" : "♥");
        addNavButton(side, "Impact", "↗");

        side.add(Box.createVerticalGlue());

        RoundedPanel account = new RoundedPanel(18, new Color(255, 255, 255, 18));
        account.setLayout(new BoxLayout(account, BoxLayout.Y_AXIS));
        account.setBorder(new EmptyBorder(13, 14, 13, 14));
        account.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        account.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(currentUser.name);
        name.setForeground(Color.WHITE);
        name.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel role = new JLabel(currentUser.isFarmer() ? "Farmer account" : "Community member");
        role.setForeground(new Color(183, 207, 190));
        role.setFont(new Font("SansSerif", Font.PLAIN, 11));
        account.add(name);
        account.add(Box.createVerticalStrut(3));
        account.add(role);

        side.add(account);
        return side;
    }

    private void addNavButton(JPanel side, String pageName, String icon) {
        NavButton button = new NavButton(icon + "   " + pageName);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.addActionListener(e -> showPage(pageName));
        navButtons.put(pageName, button);
        side.add(button);
        side.add(Box.createVerticalStrut(6));
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new EmptyBorder(13, 28, 13, 30));

        JLabel message = new JLabel(currentUser.isFarmer()
                ? "Good to see you, " + firstName(currentUser.name) + "."
                : "Welcome back, " + firstName(currentUser.name) + ".");
        message.setFont(new Font("SansSerif", Font.BOLD, 15));
        message.setForeground(TEXT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);

        JLabel rolePill = pill(currentUser.isFarmer() ? "FARMER" : "COMMUNITY", SAGE, FOREST_DARK);
        RoundedButton logout = new RoundedButton("Log out", new Color(242, 244, 242), TEXT);
        logout.setCompact(true);
        logout.addActionListener(e -> {
            saveData();
            currentUser = null;
            showLoginScreen();
        });

        actions.add(rolePill);
        actions.add(logout);
        bar.add(message, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private void showPage(String name) {
        if (pagePanel == null) return;
        pageLayout.show(pagePanel, name);
        for (Map.Entry<String, NavButton> entry : navButtons.entrySet()) {
            entry.getValue().setNavSelected(entry.getKey().equals(name));
        }
    }

    private String firstName(String name) {
        if (name == null || name.trim().isEmpty()) return "there";
        String[] pieces = name.trim().split("\\s+");
        return pieces[0];
    }

    // ============================================================
    // HOME
    // ============================================================

    private JPanel createHomePanel() {
        JPanel content = pageContent();

        ShadowPanel hero = new ShadowPanel(26, FOREST);
        hero.setLayout(new BorderLayout(26, 0));
        hero.setBorder(new EmptyBorder(28, 32, 28, 32));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel eyebrow = smallLabel(
                currentUser.isFarmer() ? "YOUR FARM DASHBOARD" : "LOCAL FOOD, CLOSER TO HOME",
                new Color(197, 226, 207)
        );

        JTextArea heroTitle = textArea(
                currentUser.isFarmer()
                        ? "Make every part of your harvest count."
                        : "Find fresh food before it goes to waste.",
                FONT_HERO_TITLE,
                Color.WHITE
        );
        heroTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_HERO_TITLE));
        heroTitle.setRows(2);
        heroTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        JTextArea desc = textArea(
                currentUser.isFarmer()
                        ? "Analyze surplus with weather and visual signals, match it to nearby demand, and move food before it becomes waste."
                        : "Browse produce from growers around Waller, save favorites, and reserve local food with a few clicks.",
                FONT_SUBTITLE,
                new Color(226, 238, 230)
        );
        desc.setRows(2);
        desc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JPanel heroButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        heroButtons.setOpaque(false);
        heroButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton primary = new RoundedButton(
                currentUser.isFarmer() ? "Post produce" : "Browse marketplace",
                Color.WHITE,
                FOREST_DARK
        );
        primary.addActionListener(e -> showPage(currentUser.isFarmer() ? "My Farm" : "Marketplace"));

        RoundedButton helper = new RoundedButton("Try Harvest Helper", new Color(255, 255, 255, 38), Color.WHITE);
        helper.addActionListener(e -> showPage("Harvest Helper"));

        heroButtons.add(primary);
        heroButtons.add(Box.createHorizontalStrut(10));
        heroButtons.add(helper);
        if (currentUser.isFarmer()) {
            heroButtons.add(Box.createHorizontalStrut(10));
            RoundedButton match = new RoundedButton("Open Smart Match", new Color(255, 255, 255, 38), Color.WHITE);
            match.addActionListener(e -> showPage("Smart Match"));
            heroButtons.add(match);
        }

        heroText.add(eyebrow);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(heroTitle);
        heroText.add(Box.createVerticalStrut(8));
        heroText.add(desc);
        heroText.add(Box.createVerticalStrut(15));
        heroText.add(heroButtons);

        ProduceIllustrationPanel art = new ProduceIllustrationPanel("Harvest");
        art.setPreferredSize(new Dimension(205, 165));
        art.setMinimumSize(new Dimension(160, 145));
        art.setOpaque(false);

        hero.add(heroText, BorderLayout.CENTER);
        hero.add(art, BorderLayout.EAST);

        content.add(hero);
        content.add(Box.createVerticalStrut(24));

        content.add(sectionHeader("At a glance", "A quick view of what's happening across HarvestHub."));
        content.add(Box.createVerticalStrut(12));

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.add(statCard("Active listings", String.valueOf(data.listings.size()), "Available right now"));
        stats.add(statCard("Produce rescued", formatNumber(data.poundsRescued) + " lbs", "Kept in the community"));
        stats.add(statCard("Families helped", String.valueOf(data.familiesHelped), "Reservations completed"));
        stats.add(statCard("Donations", String.valueOf(data.donationsCompleted), "Free-food connections"));
        content.add(stats);
        content.add(Box.createVerticalStrut(22));

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 0));
        lower.setOpaque(false);
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 198));
        lower.setAlignmentX(Component.LEFT_ALIGNMENT);

        ShadowPanel story = new ShadowPanel(22, SURFACE);
        story.setLayout(new BoxLayout(story, BoxLayout.Y_AXIS));
        story.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel storyEyebrow = smallLabel("WHY HARVESTHUB?", FOREST);
        JTextArea storyTitle = textArea("A local problem inspired a local solution.", FONT_CARD_TITLE, TEXT);
        storyTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        storyTitle.setRows(2);
        storyTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JTextArea storyText = textArea(
                "Living in Waller, Texas means being surrounded by farms and growers. HarvestHub was created to help extra produce reach nearby people instead of being left unused.",
                FONT_BODY,
                MUTED
        );
        storyText.setRows(3);
        storyText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        story.add(storyEyebrow);
        story.add(Box.createVerticalStrut(7));
        story.add(storyTitle);
        story.add(Box.createVerticalStrut(8));
        story.add(storyText);

        ShadowPanel helperPreview = new ShadowPanel(22, SAGE_LIGHT);
        helperPreview.setLayout(new BoxLayout(helperPreview, BoxLayout.Y_AXIS));
        helperPreview.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel helperEyebrow = smallLabel("NEW • HARVEST HELPER", FOREST);
        JTextArea helperTitle = textArea("Not sure what to do with extra produce?", FONT_CARD_TITLE, TEXT);
        helperTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        helperTitle.setRows(2);
        helperTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JTextArea helperText = textArea(
                "Answer a few questions and HarvestHub will estimate how urgently your harvest should be listed or donated.",
                FONT_BODY,
                MUTED
        );
        helperText.setRows(2);
        helperText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        RoundedButton tryIt = new RoundedButton("Open Harvest Helper", FOREST, Color.WHITE);
        tryIt.setAlignmentX(Component.LEFT_ALIGNMENT);
        tryIt.addActionListener(e -> showPage("Harvest Helper"));

        helperPreview.add(helperEyebrow);
        helperPreview.add(Box.createVerticalStrut(7));
        helperPreview.add(helperTitle);
        helperPreview.add(Box.createVerticalStrut(8));
        helperPreview.add(helperText);
        helperPreview.add(Box.createVerticalGlue());
        helperPreview.add(tryIt);

        lower.add(story);
        lower.add(helperPreview);
        content.add(lower);

        return wrapScrollable(content);
    }

    private JPanel statCard(String label, String value, String note) {
        ShadowPanel p = new ShadowPanel(20, SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(17, 18, 14, 18));

        JLabel l = smallLabel(label.toUpperCase(), MUTED);
        JLabel v = new JLabel(value);
        v.setForeground(TEXT);
        v.setFont(new Font("SansSerif", Font.BOLD, FONT_STAT_VALUE));
        v.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel n = new JLabel(note);
        n.setForeground(MUTED);
        n.setFont(new Font("SansSerif", Font.PLAIN, 11));
        n.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(v);
        p.add(Box.createVerticalStrut(4));
        p.add(n);
        return p;
    }

    // ============================================================
    // MARKETPLACE
    // ============================================================

    private JPanel createMarketplacePanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));

        JPanel header = new JPanel(new BorderLayout(14, 12));
        header.setOpaque(false);
        header.add(titleBlock("Marketplace", "Fresh produce from growers around the Waller area."), BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 2));
        controls.setOpaque(false);
        marketplaceSearchField = styledTextField();
        marketplaceSearchField.setPreferredSize(new Dimension(230, 40));
        marketplaceFilter = styledCombo(new String[]{"All listings", "Free donations", "Urgent rescue", "For sale"});
        marketplaceFilter.setPreferredSize(new Dimension(145, 40));
        marketplaceSort = styledCombo(new String[]{"Recommended", "Nearest to me", "Urgent first", "Free first", "Newest"});
        marketplaceSort.setPreferredSize(new Dimension(145, 40));
        controls.add(marketplaceSearchField);
        controls.add(marketplaceFilter);
        controls.add(marketplaceSort);
        header.add(controls, BorderLayout.EAST);

        marketplaceGrid = new JPanel(new GridLayout(0, 2, 14, 14));
        marketplaceGrid.setOpaque(false);

        ViewportWidthPanel gridWrapper = new ViewportWidthPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(marketplaceGrid, BorderLayout.NORTH);

        JScrollPane scroll = cleanScroll(gridWrapper);

        marketplaceSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshMarketplaceCards(); }
            public void removeUpdate(DocumentEvent e) { refreshMarketplaceCards(); }
            public void changedUpdate(DocumentEvent e) { refreshMarketplaceCards(); }
        });
        marketplaceFilter.addActionListener(e -> refreshMarketplaceCards());
        marketplaceSort.addActionListener(e -> refreshMarketplaceCards());

        page.add(header, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);
        refreshMarketplaceCards();
        return page;
    }

    private void refreshMarketplaceCards() {
        if (marketplaceGrid == null) return;
        marketplaceGrid.removeAll();

        String query = marketplaceSearchField == null ? "" : marketplaceSearchField.getText().trim().toLowerCase();
        String filter = marketplaceFilter == null ? "All listings" : String.valueOf(marketplaceFilter.getSelectedItem());
        String sort = marketplaceSort == null ? "Recommended" : String.valueOf(marketplaceSort.getSelectedItem());
        int shown = 0;

        ArrayList<ProduceListing> visible = new ArrayList<>();
        for (ProduceListing listing : data.listings) {
            String publicLocation = displayLocationForListing(listing).toLowerCase();
            boolean matchesSearch = query.isEmpty()
                    || listing.crop.toLowerCase().contains(query)
                    || listing.farmName.toLowerCase().contains(query)
                    || publicLocation.contains(query);

            boolean matchesFilter = true;
            if ("Free donations".equals(filter)) matchesFilter = listing.donation;
            else if ("Urgent rescue".equals(filter)) matchesFilter = listing.urgent;
            else if ("For sale".equals(filter)) matchesFilter = !listing.donation;

            if (matchesSearch && matchesFilter) visible.add(listing);
        }

        if ("Nearest to me".equals(sort) && currentUser != null && !currentUser.isFarmer()) {
            visible.sort(Comparator.comparingDouble(listing -> {
                double d = distanceFromCurrentUser(listing);
                return d < 0 ? Double.MAX_VALUE : d;
            }));
        } else if ("Urgent first".equals(sort)) {
            visible.sort(Comparator.comparing((ProduceListing l) -> !l.urgent));
        } else if ("Free first".equals(sort)) {
            visible.sort(Comparator.comparing((ProduceListing l) -> !l.donation));
        } else if ("Newest".equals(sort)) {
            visible.sort(Comparator.comparingInt((ProduceListing l) -> l.id).reversed());
        } else {
            visible.sort(Comparator
                    .comparing((ProduceListing l) -> !l.urgent)
                    .thenComparing((ProduceListing l) -> !l.donation)
                    .thenComparingInt(l -> -l.id));
        }

        for (ProduceListing listing : visible) {
            marketplaceGrid.add(createProduceCard(listing));
            shown++;
        }

        if (shown == 0) {
            marketplaceGrid.add(emptyState("No matches yet", "Try another crop, town, or filter."));
        }

        marketplaceGrid.revalidate();
        marketplaceGrid.repaint();
    }

    private JPanel createProduceCard(ProduceListing listing) {
        ShadowPanel card = new ShadowPanel(22, SURFACE);
        card.setLayout(new BorderLayout(14, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(360, 205));
        card.setMinimumSize(new Dimension(0, 195));

        ProduceIllustrationPanel image = new ProduceIllustrationPanel(listing.crop);
        image.setPreferredSize(new Dimension(110, 110));
        card.add(image, BorderLayout.WEST);

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        if (listing.urgent) badges.add(pill("RESCUE", SOFT_RED, RESCUE));
        if (listing.donation) badges.add(pill("FREE", SOFT_GREEN, FOREST_DARK));
        if (!listing.urgent && !listing.donation) badges.add(pill("LOCAL", SAGE_LIGHT, FOREST));

        JLabel crop = new JLabel(listing.crop);
        crop.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        crop.setForeground(TEXT);
        crop.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel farm = new JLabel(listing.farmName);
        farm.setFont(new Font("SansSerif", Font.BOLD, 13));
        farm.setForeground(FOREST);
        farm.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel location = new JLabel(marketplaceLocationText(listing) + "  •  " + formatNumber(listing.quantity) + " lbs");
        location.setFont(new Font("SansSerif", Font.PLAIN, 12));
        location.setForeground(MUTED);
        location.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel deadline = new JLabel("Pickup by " + listing.deadline);
        deadline.setFont(new Font("SansSerif", Font.PLAIN, 12));
        deadline.setForeground(MUTED);
        deadline.setAlignmentX(Component.LEFT_ALIGNMENT);

        middle.add(badges);
        middle.add(Box.createVerticalStrut(5));
        middle.add(crop);
        middle.add(Box.createVerticalStrut(4));
        middle.add(farm);
        middle.add(Box.createVerticalStrut(7));
        middle.add(location);
        middle.add(Box.createVerticalStrut(3));
        middle.add(deadline);
        card.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        JLabel price = new JLabel(listing.donation ? "FREE" : String.format("$%.2f / lb", listing.price));
        price.setFont(new Font("SansSerif", Font.BOLD, 16));
        price.setForeground(listing.donation ? FOREST : TEXT);
        bottom.add(price, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        if (!currentUser.isFarmer()) {
            RoundedButton save = new RoundedButton(currentUser.favoriteListingIds.contains(listing.id) ? "Saved ♥" : "Save ♡",
                    new Color(241, 243, 241), TEXT);
            save.setCompact(true);
            save.addActionListener(e -> toggleFavorite(listing));
            RoundedButton reserve = new RoundedButton("Reserve", FOREST, Color.WHITE);
            reserve.setCompact(true);
            reserve.addActionListener(e -> reserveListing(listing));
            buttons.add(save);
            buttons.add(reserve);
        } else if (listing.ownerUserId == currentUser.id) {
            buttons.add(pill("YOUR LISTING", SAGE, FOREST_DARK));
        }
        bottom.add(buttons, BorderLayout.EAST);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void reserveListing(ProduceListing listing) {
        String input = JOptionPane.showInputDialog(this,
                "How many pounds of " + listing.crop + " would you like to reserve?\nAvailable: " +
                        formatNumber(listing.quantity) + " lbs");
        if (input == null) return;

        try {
            double amount = Double.parseDouble(input.trim());
            if (amount <= 0) throw new NumberFormatException();
            if (amount > listing.quantity) {
                JOptionPane.showMessageDialog(this, "That is more than the available amount.");
                return;
            }

            listing.quantity -= amount;
            data.poundsRescued += amount;
            data.familiesHelped++;
            if (listing.donation) data.donationsCompleted++;

            double total = listing.donation ? 0 : amount * listing.price;
            String message = listing.donation
                    ? "Reserved! " + formatNumber(amount) + " lbs of " + listing.crop + " are yours for free.\nPickup: " + listing.location
                    : String.format("Reserved! %.1f lbs of %s\nTotal: $%.2f\nPickup: %s",
                    amount, listing.crop, total, listing.location);

            if (listing.quantity <= 0.0001) data.listings.remove(listing);
            saveData();
            refreshAllDynamicPanels();
            JOptionPane.showMessageDialog(this, message, "Reservation complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a number greater than zero.");
        }
    }

    private void toggleFavorite(ProduceListing listing) {
        if (currentUser.favoriteListingIds.contains(listing.id)) currentUser.favoriteListingIds.remove(listing.id);
        else currentUser.favoriteListingIds.add(listing.id);
        saveData();
        refreshAllDynamicPanels();
    }

    // ============================================================
    // HARVEST HELPER
    // ============================================================

    private JPanel createHarvestHelperPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));

        JPanel heading = titleBlock(
                "Harvest Helper",
                "Answer a few questions about extra produce and get a clear recommendation for what to do next."
        );
        page.add(heading, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);

        // ---------------- FORM CARD ----------------
        ShadowPanel formCard = new ShadowPanel(24, SURFACE);
        formCard.setBorder(new EmptyBorder(22, 22, 22, 22));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel formEyebrow = smallLabel("STEP 1 • HARVEST DETAILS", FOREST);
        JLabel formTitle = cardTitle("Tell us what you have");
        JTextArea formSubtitle = textArea(
                "Use the closest answer for each field. You can change anything and analyze again.",
                FONT_BODY,
                MUTED
        );
        formSubtitle.setRows(2);
        formSubtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JComboBox<String> produceBox = styledCombo(new String[]{
                "Berries", "Leafy greens", "Tomatoes", "Peaches / stone fruit",
                "Cucumbers", "Melons", "Apples", "Root vegetables", "Other produce"
        });
        JTextField quantityField = styledTextField();
        quantityField.setText("25");

        JComboBox<String> ageBox = styledCombo(new String[]{
                "Harvested today", "1–2 days ago", "3–4 days ago", "5–7 days ago", "More than a week ago"
        });
        JComboBox<String> storageBox = styledCombo(new String[]{
                "Refrigerated", "Cool indoor storage", "Room temperature indoors", "Outdoors / unrefrigerated"
        });
        JComboBox<String> conditionBox = styledCombo(new String[]{
                "Fresh and firm", "Ripe and ready", "Very ripe / soft", "Bruised or damaged", "Mold, rot, or contamination signs"
        });
        JComboBox<String> buyerBox = styledCombo(new String[]{
                "No buyer yet", "Yes, someone is already interested"
        });

        JPanel fieldGrid = new JPanel(new GridLayout(3, 2, 13, 10));
        fieldGrid.setOpaque(false);
        fieldGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        fieldGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        fieldGrid.add(labeledField("Produce type", produceBox));
        fieldGrid.add(labeledField("Amount (lbs)", quantityField));
        fieldGrid.add(labeledField("When was it harvested?", ageBox));
        fieldGrid.add(labeledField("How is it being stored?", storageBox));
        fieldGrid.add(labeledField("What condition is it in?", conditionBox));
        fieldGrid.add(labeledField("Buyer already interested?", buyerBox));

        RoundedPanel donationRow = new RoundedPanel(16, SAGE_LIGHT);
        donationRow.setLayout(new BorderLayout(10, 0));
        donationRow.setBorder(new EmptyBorder(12, 14, 12, 14));
        donationRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        donationRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JCheckBox donateBox = new JCheckBox("I would be willing to donate this produce");
        donateBox.setOpaque(false);
        donateBox.setFont(new Font("SansSerif", Font.BOLD, FONT_BODY));
        donateBox.setForeground(TEXT);
        donateBox.setFocusPainted(false);
        donationRow.add(donateBox, BorderLayout.CENTER);

        RoundedPanel weatherRow = new RoundedPanel(16, new Color(238, 245, 248));
        weatherRow.setLayout(new BorderLayout(10, 0));
        weatherRow.setBorder(new EmptyBorder(11, 14, 11, 14));
        weatherRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        weatherRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JPanel weatherCopy = new JPanel();
        weatherCopy.setOpaque(false);
        weatherCopy.setLayout(new BoxLayout(weatherCopy, BoxLayout.Y_AXIS));
        JLabel weatherTitle = new JLabel("Waller weather signal");
        weatherTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_BODY));
        weatherTitle.setForeground(TEXT);
        JTextArea weatherReading = textArea(
                "Loading local temperature and humidity…",
                11,
                MUTED
        );
        weatherReading.setRows(3);
        weatherReading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        weatherCopy.add(weatherTitle);
        weatherCopy.add(Box.createVerticalStrut(3));
        weatherCopy.add(weatherReading);

        RoundedButton refreshWeather = new RoundedButton("Refresh", SURFACE, FOREST_DARK);
        refreshWeather.setCompact(true);
        weatherRow.add(weatherCopy, BorderLayout.CENTER);
        weatherRow.add(refreshWeather, BorderLayout.EAST);

        JPanel formActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formActions.setOpaque(false);
        formActions.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton analyze = new RoundedButton("Analyze my harvest", FOREST, Color.WHITE);
        RoundedButton reset = new RoundedButton("Reset form", new Color(241, 243, 241), TEXT);
        formActions.add(analyze);
        formActions.add(Box.createHorizontalStrut(9));
        formActions.add(reset);

        formCard.add(formEyebrow);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(formTitle);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(formSubtitle);
        formCard.add(Box.createVerticalStrut(15));
        formCard.add(fieldGrid);
        formCard.add(Box.createVerticalStrut(11));
        formCard.add(donationRow);
        formCard.add(Box.createVerticalStrut(9));
        formCard.add(weatherRow);
        formCard.add(Box.createVerticalGlue());
        formCard.add(formActions);

        // ---------------- RESULT CARD ----------------
        ShadowPanel resultCard = new ShadowPanel(24, SAGE_LIGHT);
        resultCard.setBorder(new EmptyBorder(22, 24, 22, 24));
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));

        JLabel resultEyebrow = smallLabel("STEP 2 • YOUR RECOMMENDATION", FOREST);
        JLabel resultTitle = new JLabel("Your result will appear here");
        resultTitle.setFont(new Font("SansSerif", Font.BOLD, 23));
        resultTitle.setForeground(TEXT);
        resultTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea resultSummary = textArea(
                "Harvest Helper will estimate how urgently your produce should be listed, discounted, donated, or held for a normal pickup window.",
                FONT_SUBTITLE,
                MUTED
        );
        resultSummary.setRows(5);
        resultSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));

        JPanel scoreHeader = new JPanel(new BorderLayout());
        scoreHeader.setOpaque(false);
        scoreHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel scoreLabel = new JLabel("Urgency score");
        scoreLabel.setFont(new Font("SansSerif", Font.BOLD, FONT_FIELD_LABEL));
        scoreLabel.setForeground(TEXT);
        JLabel scoreHint = new JLabel("0 = relaxed   •   100 = immediate attention");
        scoreHint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        scoreHint.setForeground(MUTED);
        scoreHeader.add(scoreLabel, BorderLayout.WEST);
        scoreHeader.add(scoreHint, BorderLayout.EAST);

        JProgressBar scoreBar = new JProgressBar(0, 100);
        scoreBar.setValue(0);
        scoreBar.setStringPainted(true);
        scoreBar.setString("Not analyzed");
        scoreBar.setForeground(FOREST);
        scoreBar.setBackground(new Color(226, 232, 227));
        scoreBar.setBorderPainted(false);
        scoreBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel actionWindowLabel = new JLabel("Estimated action window: analyze to calculate");
        actionWindowLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        actionWindowLabel.setForeground(MUTED);
        actionWindowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedPanel recommendationBox = new RoundedPanel(18, SURFACE);
        recommendationBox.setBorder(new EmptyBorder(16, 18, 16, 18));
        recommendationBox.setLayout(new BoxLayout(recommendationBox, BoxLayout.Y_AXIS));
        recommendationBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        recommendationBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel recommendationTitle = cardTitle("Recommended next steps");
        JTextArea recommendationText = textArea(
                "Your next steps will appear here after you analyze the harvest.",
                FONT_BODY,
                MUTED
        );
        recommendationText.setRows(5);
        recommendationText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        recommendationBox.add(recommendationTitle);
        recommendationBox.add(Box.createVerticalStrut(7));
        recommendationBox.add(recommendationText);

        RoundedPanel caution = new RoundedPanel(16, new Color(247, 244, 235));
        caution.setBorder(new EmptyBorder(12, 14, 12, 14));
        caution.setLayout(new BorderLayout(10, 0));
        caution.setAlignmentX(Component.LEFT_ALIGNMENT);
        caution.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        JLabel cautionIcon = new JLabel("ⓘ");
        cautionIcon.setFont(new Font("SansSerif", Font.BOLD, 16));
        cautionIcon.setForeground(ACCENT.darker());
        cautionIcon.setVerticalAlignment(SwingConstants.TOP);

        JTextArea cautionText = textArea(
                "Harvest Helper estimates listing urgency, not food safety. Never distribute produce showing mold, rot, unusual odors, contamination, or other signs that it may be unsafe.",
                11,
                new Color(103, 91, 70)
        );
        cautionText.setRows(3);
        cautionText.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        caution.add(cautionIcon, BorderLayout.WEST);
        caution.add(cautionText, BorderLayout.CENTER);

        JPanel intelligenceActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        intelligenceActions.setOpaque(false);
        intelligenceActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        RoundedButton launchListing = new RoundedButton("Launch recommended listing", FOREST, Color.WHITE);
        RoundedButton findMatches = new RoundedButton("Find best matches", SAGE, FOREST_DARK);
        launchListing.setEnabled(false);
        findMatches.setEnabled(false);
        intelligenceActions.add(launchListing);
        intelligenceActions.add(Box.createHorizontalStrut(8));
        intelligenceActions.add(findMatches);

        final HarvestRecommendation[] latestRecommendation = new HarvestRecommendation[1];

        resultCard.add(resultEyebrow);
        resultCard.add(Box.createVerticalStrut(7));
        resultCard.add(resultTitle);
        resultCard.add(Box.createVerticalStrut(8));
        resultCard.add(resultSummary);
        resultCard.add(Box.createVerticalStrut(14));
        resultCard.add(scoreHeader);
        resultCard.add(Box.createVerticalStrut(6));
        resultCard.add(scoreBar);
        resultCard.add(Box.createVerticalStrut(7));
        resultCard.add(actionWindowLabel);
        resultCard.add(Box.createVerticalStrut(12));
        resultCard.add(recommendationBox);
        resultCard.add(Box.createVerticalStrut(10));
        if (currentUser.isFarmer()) resultCard.add(intelligenceActions);
        resultCard.add(Box.createVerticalGlue());
        resultCard.add(caution);

        Runnable loadWeather = () -> refreshWallerWeather(weatherReading, refreshWeather);
        refreshWeather.addActionListener(e -> loadWeather.run());
        loadWeather.run();

        analyze.addActionListener(e -> {
            try {
                double quantity = Double.parseDouble(quantityField.getText().trim());
                if (quantity <= 0) throw new NumberFormatException();

                HarvestRecommendation rec = analyzeHarvest(
                        String.valueOf(produceBox.getSelectedItem()),
                        quantity,
                        String.valueOf(ageBox.getSelectedItem()),
                        String.valueOf(storageBox.getSelectedItem()),
                        String.valueOf(conditionBox.getSelectedItem()),
                        String.valueOf(buyerBox.getSelectedItem()),
                        donateBox.isSelected(),
                        currentWeather,
                        latestVisualRiskAdjustment
                );
                latestRecommendation[0] = rec;

                resultEyebrow.setText(rec.eyebrow);
                resultEyebrow.setForeground(rec.color);
                resultTitle.setText(rec.title);
                resultTitle.setForeground(rec.color);
                resultSummary.setText(rec.summary + "\n\n" +
                        buildWeatherContext(
                                String.valueOf(produceBox.getSelectedItem()),
                                String.valueOf(storageBox.getSelectedItem()),
                                currentWeather
                        ));
                scoreBar.setValue(rec.score);
                scoreBar.setString(rec.score + " / 100");
                scoreBar.setForeground(rec.color);
                actionWindowLabel.setText("Estimated action window: " + rec.actionWindow);
                actionWindowLabel.setForeground(rec.color);
                recommendationText.setText(rec.actions);
                launchListing.setEnabled(currentUser.isFarmer() && rec.safeToList);
                findMatches.setEnabled(currentUser.isFarmer() && rec.safeToList && !data.recipients.isEmpty());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid quantity greater than zero.",
                        "Check the form",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        reset.addActionListener(e -> {
            produceBox.setSelectedIndex(0);
            quantityField.setText("25");
            ageBox.setSelectedIndex(0);
            storageBox.setSelectedIndex(0);
            conditionBox.setSelectedIndex(0);
            buyerBox.setSelectedIndex(0);
            donateBox.setSelected(false);

            resultEyebrow.setText("STEP 2 • YOUR RECOMMENDATION");
            resultEyebrow.setForeground(FOREST);
            resultTitle.setText("Your result will appear here");
            resultTitle.setForeground(TEXT);
            resultSummary.setText("Harvest Helper will estimate how urgently your produce should be listed, discounted, donated, or held for a normal pickup window.");
            scoreBar.setValue(0);
            scoreBar.setString("Not analyzed");
            scoreBar.setForeground(FOREST);
            actionWindowLabel.setText("Estimated action window: analyze to calculate");
            actionWindowLabel.setForeground(MUTED);
            recommendationText.setText("Your next steps will appear here after you analyze the harvest.");
            latestRecommendation[0] = null;
            launchListing.setEnabled(false);
            findMatches.setEnabled(false);
        });

        launchListing.addActionListener(e -> {
            HarvestRecommendation rec = latestRecommendation[0];
            if (rec == null || !rec.safeToList) return;
            try {
                double quantity = Double.parseDouble(quantityField.getText().trim());
                String produce = String.valueOf(produceBox.getSelectedItem());
                launchListingFromHelper(produce, quantity, rec, donateBox.isSelected());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please analyze a valid quantity first.");
            }
        });

        findMatches.addActionListener(e -> {
            HarvestRecommendation rec = latestRecommendation[0];
            if (rec == null || !rec.safeToList) return;
            if (lastMatchedListing == null || !lastMatchedListing.crop.equalsIgnoreCase(String.valueOf(produceBox.getSelectedItem()))) {
                JOptionPane.showMessageDialog(this,
                        "Launch the recommended listing first so Smart Match has a real HarvestHub listing to optimize.",
                        "Smart Match", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showPage("Smart Match");
            refreshSmartMatchListings();
            selectSmartMatchListing(lastMatchedListing.id);
            runSmartMatch();
        });

        columns.add(formCard);
        columns.add(resultCard);
        page.add(columns, BorderLayout.CENTER);
        return page;
    }

    private HarvestRecommendation analyzeHarvest(String produce, double quantity, String age,
                                                 String storage, String condition, String buyer,
                                                 boolean willingToDonate, WeatherSnapshot weather,
                                                 int visualRiskAdjustment) {
        int score = 0;
        String lower = produce.toLowerCase();

        // Produce that tends to have a shorter quality window starts with a higher urgency score.
        if (lower.contains("berries") || lower.contains("leafy")) score += 24;
        else if (lower.contains("tomato") || lower.contains("peach") || lower.contains("cucumber")) score += 17;
        else if (lower.contains("melon")) score += 12;
        else if (lower.contains("apple")) score += 8;
        else if (lower.contains("root")) score += 6;
        else score += 12;

        if (age.startsWith("1")) score += 9;
        else if (age.startsWith("3")) score += 23;
        else if (age.startsWith("5")) score += 38;
        else if (age.startsWith("More")) score += 55;

        if (storage.startsWith("Refrigerated")) score -= 8;
        else if (storage.startsWith("Room")) score += 11;
        else if (storage.startsWith("Outdoors")) score += 23;

        if (condition.startsWith("Ripe")) score += 7;
        else if (condition.startsWith("Very")) score += 24;
        else if (condition.startsWith("Bruised")) score += 34;
        else if (condition.startsWith("Mold")) score = 100;
        else score -= 7;

        if (quantity >= 100) score += 10;
        else if (quantity >= 50) score += 6;
        else if (quantity >= 25) score += 3;

        if (buyer.startsWith("Yes")) score -= 9;

        // Local conditions matter most when produce is not refrigerated.
        if (weather != null && !storage.startsWith("Refrigerated")) {
            if (weather.temperatureF >= 95) score += 15;
            else if (weather.temperatureF >= 90) score += 10;
            else if (weather.temperatureF >= 85) score += 6;
            if (weather.humidity >= 80 && (lower.contains("berries") || lower.contains("leafy"))) score += 5;
        }

        // HarvestScan is a visible-quality signal only. Keep its influence modest.
        score += Math.max(0, Math.min(18, visualRiskAdjustment));
        score = Math.max(0, Math.min(100, score));

        if (condition.startsWith("Mold")) {
            return new HarvestRecommendation(
                    100,
                    "DO NOT LIST",
                    "Do not distribute this produce",
                    "The condition selected includes signs that the produce may not be appropriate to sell or donate. HarvestHub should not be used to pass questionable food to another person.",
                    "• Do not create a marketplace or rescue listing.\n" +
                            "• Separate the affected produce from food you plan to distribute.\n" +
                            "• Consider composting or another appropriate disposal option.\n" +
                            "• Follow applicable food-safety guidance if you are unsure.",
                    RESCUE
            );
        }

        if (score < 25) {
            return new HarvestRecommendation(
                    score,
                    "LOW URGENCY",
                    "You have some breathing room",
                    "Based on the information you entered, this harvest does not appear to need an urgent rescue listing right now.",
                    "• Create a normal marketplace listing.\n" +
                            "• Use a regular pickup window instead of an emergency deadline.\n" +
                            "• Recheck the condition later if it has not been claimed.",
                    FOREST
            );
        }

        if (score < 50) {
            String donationLine = willingToDonate
                    ? "• You are open to donating, so consider switching to FREE if it is not claimed soon.\n"
                    : "• Consider a small price reduction if the listing gets little interest.\n";
            return new HarvestRecommendation(
                    score,
                    "PLAN SOON",
                    "List it while it still has time",
                    "A few factors are increasing urgency. Posting now gives neighbors more time to reserve the produce before its quality declines.",
                    "• Post the harvest today as a normal listing.\n" + donationLine +
                            "• Choose a clear pickup deadline within the next few days.",
                    new Color(179, 121, 49)
            );
        }

        if (score < 72) {
            String donationLine = willingToDonate
                    ? "• Mark it FREE if your priority is getting it picked up quickly.\n"
                    : "• Lowering the price may help it move faster.\n";
            return new HarvestRecommendation(
                    score,
                    "RESCUE SOON",
                    "Try to move this harvest quickly",
                    "The age, condition, storage, or amount you entered suggests this produce may benefit from a faster pickup plan.",
                    "• Use a short pickup deadline, ideally today or tomorrow.\n" + donationLine +
                            "• Consider using Rescue My Harvest if it remains unclaimed.",
                    ACCENT.darker()
            );
        }

        String donationLine = willingToDonate
                ? "• A free Rescue My Harvest listing may give it the best chance of being claimed quickly.\n"
                : "• Consider whether donating is preferable to letting the harvest go unused.\n";
        return new HarvestRecommendation(
                score,
                "HIGH URGENCY",
                "A rescue listing makes sense",
                "Several factors point to a short window for getting this harvest into someone else's hands. Focus on speed rather than waiting for the perfect buyer.",
                "• Post a Rescue My Harvest listing now.\n" + donationLine +
                        "• Use same-day or next-day pickup when practical.\n" +
                        "• Recheck the produce before handoff and do not distribute anything showing spoilage or contamination.",
                RESCUE
        );
    }

    private static class HarvestRecommendation {
        int score;
        String eyebrow;
        String title;
        String summary;
        String actions;
        String actionWindow;
        boolean safeToList;
        Color color;

        HarvestRecommendation(int score, String eyebrow, String title, String summary, String actions, Color color) {
            this.score = score;
            this.eyebrow = eyebrow;
            this.title = title;
            this.summary = summary;
            this.actions = actions;
            this.color = color;
            this.safeToList = !"DO NOT LIST".equals(eyebrow);
            if (!safeToList) this.actionWindow = "Do not distribute";
            else if (score >= 72) this.actionWindow = "Same day to 24 hours";
            else if (score >= 50) this.actionWindow = "Within 1–2 days";
            else if (score >= 25) this.actionWindow = "Within 2–3 days";
            else this.actionWindow = "Normal pickup window • about 3–5 days";
        }
    }


    // ============================================================
    // LOCAL WEATHER + ONE-CLICK INTELLIGENCE ACTIONS
    // ============================================================

    private void refreshWallerWeather(JTextArea weatherReading, JButton refreshButton) {
        refreshButton.setEnabled(false);
        weatherReading.setText("Refreshing Waller conditions…");

        SwingWorker<WeatherSnapshot, Void> worker = new SwingWorker<>() {
            @Override
            protected WeatherSnapshot doInBackground() throws Exception {
                return fetchWallerWeather();
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    currentWeather = get();
                    weatherReading.setText(String.format(Locale.US,
                            "Now %.0f°F • %.0f%% humidity • today's high %.0f°F. %s",
                            currentWeather.temperatureF,
                            currentWeather.humidity,
                            currentWeather.highF,
                            generalWeatherExplanation(currentWeather)));
                } catch (Exception ex) {
                    weatherReading.setText("Weather unavailable. Harvest Helper will use your harvest details without a live weather adjustment.");
                }
            }
        };
        worker.execute();
    }

    private String generalWeatherExplanation(WeatherSnapshot weather) {
        if (weather == null) return "";
        if (weather.temperatureF >= 95 || weather.highF >= 98) {
            return "Very hot conditions can speed ripening, softening, and moisture loss when produce is not refrigerated.";
        }
        if (weather.temperatureF >= 88 || weather.highF >= 92) {
            return "Warm conditions can shorten the quality window for unrefrigerated produce, especially ripe fruit and delicate greens.";
        }
        if (weather.humidity >= 80) {
            return "Humidity is high, so delicate produce may lose quality faster if it is not kept appropriately cool and dry.";
        }
        if (weather.temperatureF <= 60) {
            return "Cooler outdoor conditions are adding little heat-related urgency right now.";
        }
        return "Conditions are fairly moderate, so weather is adding only a small amount of urgency for most properly stored produce.";
    }

    private String buildWeatherContext(String produce, String storage, WeatherSnapshot weather) {
        if (weather == null) {
            return "Weather context: Live Waller weather is unavailable right now, so this recommendation is based on the harvest details you entered.";
        }

        String lowerProduce = produce == null ? "" : produce.toLowerCase();
        boolean refrigerated = storage != null && storage.startsWith("Refrigerated");
        boolean delicate = lowerProduce.contains("berries") || lowerProduce.contains("leafy") ||
                lowerProduce.contains("peach") || lowerProduce.contains("tomato");

        String conditions = String.format(Locale.US,
                "Weather context: Waller is currently %.0f°F with %.0f%% humidity, with a high near %.0f°F. ",
                weather.temperatureF, weather.humidity, weather.highF);

        if (refrigerated) {
            if (weather.temperatureF >= 90 || weather.highF >= 95) {
                return conditions + "It is hot outside, but because you selected refrigerated storage, Harvest Helper reduces the effect of outdoor heat as long as the produce stays properly chilled.";
            }
            return conditions + "Because you selected refrigerated storage, current outdoor weather is having only a small effect on the urgency score.";
        }

        if (weather.temperatureF >= 95 || weather.highF >= 98) {
            String extra = delicate
                    ? " For this type of produce, that can mean faster ripening or softening and a shorter window to get it picked up."
                    : " That heat can still increase moisture loss and shorten the usable quality window.";
            return conditions + "These are very hot conditions, so unrefrigerated produce can ripen and lose quality faster." + extra +
                    " Harvest Helper increased the urgency score to reflect that.";
        }

        if (weather.temperatureF >= 88 || weather.highF >= 92) {
            return conditions + "Warm weather can speed ripening and softening in unrefrigerated produce, so Harvest Helper adds some urgency rather than treating the harvest like it is being stored in cool conditions.";
        }

        if (weather.humidity >= 80 && delicate) {
            return conditions + "Humidity is high. Delicate produce can have a shorter quality window under warm, humid conditions when it is not properly chilled, so Harvest Helper adds a small weather-related urgency adjustment.";
        }

        return conditions + "The current weather is fairly moderate, so it is not adding much extra urgency beyond the age, storage, condition, and quantity you entered.";
    }

    private WeatherSnapshot fetchWallerWeather() throws IOException {
        String endpoint = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=30.0588&longitude=-95.9263" +
                "&current=temperature_2m,relative_humidity_2m" +
                "&daily=temperature_2m_max" +
                "&temperature_unit=fahrenheit" +
                "&forecast_days=1" +
                "&timezone=America%2FChicago";

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(6500);
        connection.setRequestProperty("User-Agent", "HarvestHubStudentProject/2.0");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() != 200) {
            connection.disconnect();
            throw new IOException("Weather service returned " + connection.getResponseCode());
        }

        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
        } finally {
            connection.disconnect();
        }

        double temperature = extractJsonNumber(json.toString(), "temperature_2m");
        double humidity = extractJsonNumber(json.toString(), "relative_humidity_2m");
        double high = extractJsonArrayFirstNumber(json.toString(), "temperature_2m_max");
        return new WeatherSnapshot(temperature, humidity, high, LocalDateTime.now());
    }

    private double extractJsonNumber(String json, String key) throws IOException {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) throw new IOException("Missing weather field: " + key);
        return Double.parseDouble(matcher.group(1));
    }

    private double extractJsonArrayFirstNumber(String json, String key) throws IOException {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) throw new IOException("Missing weather field: " + key);
        return Double.parseDouble(matcher.group(1));
    }

    private void launchListingFromHelper(String produce, double quantity, HarvestRecommendation rec,
                                         boolean willingToDonate) {
        if (!currentUser.isFarmer() || !rec.safeToList) return;

        String location = JOptionPane.showInputDialog(
                this,
                "Confirm the pickup area for this listing:",
                "Waller, TX"
        );
        if (location == null) return;
        location = location.trim();
        if (location.isEmpty()) location = "Waller, TX";

        boolean donation = willingToDonate && rec.score >= 45;
        double price = 0;
        if (!donation) {
            String priceInput = JOptionPane.showInputDialog(
                    this,
                    "Recommended listing type: " + (rec.score >= 50 ? "fast-moving / discounted" : "normal") +
                            "\nEnter a price per pound:",
                    rec.score >= 50 ? "0.75" : "1.25"
            );
            if (priceInput == null) return;
            try {
                price = Double.parseDouble(priceInput.trim());
                if (price < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid non-negative price.");
                return;
            }
        }

        boolean urgent = rec.score >= 50;
        int deadlineDays = rec.score >= 72 ? 1 : rec.score >= 50 ? 2 : rec.score >= 25 ? 3 : 5;
        StringBuilder note = new StringBuilder("Created from Harvest Intelligence • urgency ")
                .append(rec.score).append("/100");
        if (currentWeather != null) {
            note.append(String.format(Locale.US, " • Waller %.0f°F / %.0f%% humidity",
                    currentWeather.temperatureF, currentWeather.humidity));
        }
        if (latestVisualRiskAdjustment > 0) {
            note.append(" • HarvestScan visual signal +").append(latestVisualRiskAdjustment);
        }

        ProduceListing listing = new ProduceListing(
                data.nextListingId++,
                currentUser.id,
                produce,
                currentUser.farmName,
                quantity,
                donation ? 0 : price,
                location,
                LocalDate.now().plusDays(deadlineDays).toString(),
                donation,
                urgent,
                note.toString()
        );

        data.listings.add(0, listing);
        lastMatchedListing = listing;
        saveData();
        refreshAllDynamicPanels();
        refreshSmartMatchListings();

        JOptionPane.showMessageDialog(
                this,
                "Listing launched. HarvestHub used the intelligence result to set the urgency and pickup window.\n\n" +
                        produce + " • " + formatNumber(quantity) + " lbs • pickup by " + listing.deadline,
                "Listing created",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static class WeatherSnapshot {
        final double temperatureF;
        final double humidity;
        final double highF;
        final LocalDateTime fetchedAt;

        WeatherSnapshot(double temperatureF, double humidity, double highF, LocalDateTime fetchedAt) {
            this.temperatureF = temperatureF;
            this.humidity = humidity;
            this.highF = highF;
            this.fetchedAt = fetchedAt;
        }
    }


    // ============================================================
    // SMART MATCH + ROUTE OPTIMIZATION
    // ============================================================

    private JPanel createSmartMatchPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));
        page.add(titleBlock(
                "Smart Match",
                "Rank nearby recipient profiles by fit, urgency, capacity, pickup timing, and approximate distance."
        ), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        ShadowPanel controls = new ShadowPanel(24, SURFACE);
        controls.setPreferredSize(new Dimension(315, 0));
        controls.setBorder(new EmptyBorder(22, 22, 22, 22));
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JLabel step = smallLabel("STEP 1 • CHOOSE A HARVEST", FOREST);
        JLabel title = cardTitle("What should we match?");
        smartMatchListingBox = styledCombo(new String[]{"Loading listings…"});
        smartMatchListingBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton run = new RoundedButton("Find best matches", FOREST, Color.WHITE);
        run.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        run.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton addRecipient = new RoundedButton("+ Add recipient profile", SAGE, FOREST_DARK);
        addRecipient.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        addRecipient.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedPanel formula = new RoundedPanel(18, SAGE_LIGHT);
        formula.setBorder(new EmptyBorder(14, 15, 14, 15));
        formula.setLayout(new BoxLayout(formula, BoxLayout.Y_AXIS));
        formula.setAlignmentX(Component.LEFT_ALIGNMENT);
        formula.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        formula.add(smallLabel("MATCH SCORE", FOREST));
        formula.add(Box.createVerticalStrut(5));
        JTextArea formulaText = textArea(
                "Distance 25%\nQuantity fit 25%\nPickup timing 20%\nCrop fit 15%\nDonation compatibility 15%",
                12, TEXT
        );
        formulaText.setRows(5);
        formula.add(formulaText);

        JLabel demoNote = new JLabel("Demo profiles are clearly labeled until you add real partners.");
        demoNote.setFont(new Font("SansSerif", Font.PLAIN, 10));
        demoNote.setForeground(MUTED);
        demoNote.setAlignmentX(Component.LEFT_ALIGNMENT);

        controls.add(step);
        controls.add(Box.createVerticalStrut(6));
        controls.add(title);
        controls.add(Box.createVerticalStrut(13));
        controls.add(labeledField("Active HarvestHub listing", smartMatchListingBox));
        controls.add(Box.createVerticalStrut(12));
        controls.add(run);
        controls.add(Box.createVerticalStrut(8));
        controls.add(addRecipient);
        controls.add(Box.createVerticalStrut(18));
        controls.add(formula);
        controls.add(Box.createVerticalStrut(10));
        controls.add(demoNote);
        controls.add(Box.createVerticalGlue());

        JPanel resultsSide = new JPanel(new BorderLayout(0, 12));
        resultsSide.setOpaque(false);

        smartMatchResultsPanel = new ViewportWidthPanel();
        smartMatchResultsPanel.setOpaque(false);
        smartMatchResultsPanel.setLayout(new BoxLayout(smartMatchResultsPanel, BoxLayout.Y_AXIS));
        smartMatchResultsPanel.add(emptyState(
                "Ready to match",
                "Choose one of your listings and let HarvestHub rank recipient profiles."
        ));

        JScrollPane resultsScroll = cleanScroll(smartMatchResultsPanel);

        ShadowPanel routeCard = new ShadowPanel(22, SURFACE);
        routeCard.setBorder(new EmptyBorder(15, 18, 15, 18));
        routeCard.setLayout(new BorderLayout(14, 0));
        routeCard.setPreferredSize(new Dimension(0, 118));

        JPanel routeCopy = new JPanel();
        routeCopy.setOpaque(false);
        routeCopy.setLayout(new BoxLayout(routeCopy, BoxLayout.Y_AXIS));
        smartRouteSummaryLabel = new JLabel("Route optimizer is waiting for a match.");
        smartRouteSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        smartRouteSummaryLabel.setForeground(TEXT);
        smartRouteDetailsArea = textArea(
                "It will select enough recipients to cover the harvest, then compare a nearest-neighbor route with the original match order.",
                11, MUTED
        );
        smartRouteDetailsArea.setRows(3);
        routeCopy.add(smartRouteSummaryLabel);
        routeCopy.add(Box.createVerticalStrut(5));
        routeCopy.add(smartRouteDetailsArea);

        JPanel routeActions = new JPanel();
        routeActions.setOpaque(false);
        routeActions.setLayout(new BoxLayout(routeActions, BoxLayout.Y_AXIS));
        RoundedButton optimize = new RoundedButton("Optimize pickup route", FOREST, Color.WHITE);
        RoundedButton showMap = new RoundedButton("Show route on map", SAGE, FOREST_DARK);
        optimize.setMaximumSize(new Dimension(180, 38));
        showMap.setMaximumSize(new Dimension(180, 38));
        routeActions.add(optimize);
        routeActions.add(Box.createVerticalStrut(6));
        routeActions.add(showMap);

        routeCard.add(routeCopy, BorderLayout.CENTER);
        routeCard.add(routeActions, BorderLayout.EAST);

        resultsSide.add(resultsScroll, BorderLayout.CENTER);
        resultsSide.add(routeCard, BorderLayout.SOUTH);

        body.add(controls, BorderLayout.WEST);
        body.add(resultsSide, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);

        run.addActionListener(e -> runSmartMatch());
        addRecipient.addActionListener(e -> showAddRecipientDialog());
        optimize.addActionListener(e -> optimizeCurrentRoute());
        showMap.addActionListener(e -> {
            if (lastMatchedListing == null || lastOptimizedRoute.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Optimize a route first.");
                return;
            }
            if (communityMapPanel != null) {
                communityMapPanel.showSmartRoute(lastMatchedListing, lastOptimizedRoute);
                showPage("Community Map");
            }
        });

        refreshSmartMatchListings();
        return page;
    }

    private void refreshSmartMatchListings() {
        if (smartMatchListingBox == null || currentUser == null || !currentUser.isFarmer()) return;
        Integer selectedId = null;
        int oldIndex = smartMatchListingBox.getSelectedIndex();
        if (oldIndex >= 0 && oldIndex < smartMatchListingIds.size()) selectedId = smartMatchListingIds.get(oldIndex);

        smartMatchListingBox.removeAllItems();
        smartMatchListingIds.clear();
        for (ProduceListing listing : data.listings) {
            if (listing.ownerUserId == currentUser.id) {
                smartMatchListingIds.add(listing.id);
                smartMatchListingBox.addItem(listing.crop + " • " + formatNumber(listing.quantity) + " lbs • " +
                        (listing.urgent ? "RESCUE" : "active"));
            }
        }
        if (smartMatchListingIds.isEmpty()) {
            smartMatchListingBox.addItem("No active farm listings yet");
            smartMatchListingBox.setEnabled(false);
        } else {
            smartMatchListingBox.setEnabled(true);
            if (selectedId != null) selectSmartMatchListing(selectedId);
        }
    }

    private void selectSmartMatchListing(int listingId) {
        if (smartMatchListingBox == null) return;
        for (int i = 0; i < smartMatchListingIds.size(); i++) {
            if (smartMatchListingIds.get(i) == listingId) {
                smartMatchListingBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private ProduceListing selectedSmartMatchListing() {
        if (smartMatchListingBox == null) return null;
        int index = smartMatchListingBox.getSelectedIndex();
        if (index < 0 || index >= smartMatchListingIds.size()) return null;
        int id = smartMatchListingIds.get(index);
        for (ProduceListing listing : data.listings) if (listing.id == id) return listing;
        return null;
    }

    private void runSmartMatch() {
        if (smartMatchResultsPanel == null) return;
        ProduceListing listing = selectedSmartMatchListing();
        if (listing == null) {
            JOptionPane.showMessageDialog(this, "Create a farm listing first, then Smart Match can optimize it.");
            return;
        }

        List<MatchResult> matches = calculateMatches(listing);
        smartMatchResultsPanel.removeAll();
        lastMatchedListing = listing;
        lastOptimizedRoute = new ArrayList<>();
        smartRouteSummaryLabel.setText("Matches ready • optimize a route when you want to plan distribution.");
        smartRouteDetailsArea.setText("The route optimizer will use the highest-fit recipient profiles while covering as much of the harvest as possible.");

        JLabel heading = smallLabel("STEP 2 • RANKED RECIPIENTS", FOREST);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        smartMatchResultsPanel.add(heading);
        smartMatchResultsPanel.add(Box.createVerticalStrut(9));

        if (matches.isEmpty()) {
            smartMatchResultsPanel.add(emptyState("No compatible profiles", "Add a recipient profile or change the listing."));
        } else {
            int rank = 1;
            for (MatchResult match : matches) {
                smartMatchResultsPanel.add(createMatchResultCard(rank++, match));
                smartMatchResultsPanel.add(Box.createVerticalStrut(10));
            }
        }
        smartMatchResultsPanel.revalidate();
        smartMatchResultsPanel.repaint();
    }

    private JPanel createMatchResultCard(int rank, MatchResult match) {
        ShadowPanel card = new ShadowPanel(20, SURFACE);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setLayout(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel scoreBox = new JPanel();
        scoreBox.setOpaque(false);
        scoreBox.setLayout(new BoxLayout(scoreBox, BoxLayout.Y_AXIS));
        scoreBox.setPreferredSize(new Dimension(84, 90));
        JLabel rankLabel = smallLabel("MATCH #" + rank, MUTED);
        JLabel score = new JLabel(match.score + "%");
        score.setFont(new Font("SansSerif", Font.BOLD, 27));
        score.setForeground(match.score >= 80 ? FOREST : match.score >= 60 ? ACCENT.darker() : TEXT);
        scoreBox.add(rankLabel);
        scoreBox.add(Box.createVerticalStrut(7));
        scoreBox.add(score);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(match.recipient.name);
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        name.setForeground(TEXT);
        JLabel meta = new JLabel(match.recipient.type + " • " + match.recipient.location +
                " • ~" + String.format(Locale.US, "%.1f", match.distanceMiles) + " mi straight-line");
        meta.setFont(new Font("SansSerif", Font.PLAIN, 11));
        meta.setForeground(MUTED);
        JTextArea explanation = textArea(match.explanation, 11, TEXT);
        explanation.setRows(2);
        explanation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        copy.add(name);
        copy.add(Box.createVerticalStrut(4));
        copy.add(meta);
        copy.add(Box.createVerticalStrut(6));
        copy.add(explanation);

        JPanel fitBox = new JPanel();
        fitBox.setOpaque(false);
        fitBox.setLayout(new BoxLayout(fitBox, BoxLayout.Y_AXIS));
        fitBox.setPreferredSize(new Dimension(145, 90));
        JLabel capacity = new JLabel("Capacity " + formatNumber(match.recipient.capacityLbs) + " lbs");
        capacity.setFont(new Font("SansSerif", Font.BOLD, 12));
        capacity.setForeground(FOREST_DARK);
        JLabel timing = new JLabel(match.recipient.pickupToday ? "Pickup today" : "Pickup within 48h");
        timing.setFont(new Font("SansSerif", Font.PLAIN, 11));
        timing.setForeground(MUTED);
        JLabel demo = new JLabel(match.recipient.demo ? "PROTOTYPE PROFILE" : "USER-ADDED PROFILE");
        demo.setFont(new Font("SansSerif", Font.BOLD, 9));
        demo.setForeground(match.recipient.demo ? ACCENT.darker() : FOREST);
        fitBox.add(capacity);
        fitBox.add(Box.createVerticalStrut(5));
        fitBox.add(timing);
        fitBox.add(Box.createVerticalGlue());
        fitBox.add(demo);

        card.add(scoreBox, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        card.add(fitBox, BorderLayout.EAST);
        return card;
    }

    private List<MatchResult> calculateMatches(ProduceListing listing) {
        ArrayList<MatchResult> results = new ArrayList<>();
        Coordinate origin = coordinateForListing(listing);
        for (RecipientProfile recipient : data.recipients) {
            double distance = distanceMiles(origin.lat, origin.lon, recipient.lat, recipient.lon);
            int distanceScore = distance <= 3 ? 25 : distance <= 7 ? 21 : distance <= 12 ? 16 : distance <= 20 ? 10 : 4;
            double quantityFraction = Math.min(1.0, recipient.capacityLbs / Math.max(1.0, listing.quantity));
            int quantityScore = (int) Math.round(quantityFraction * 25);
            int timingScore = recipient.pickupToday ? 20 : listing.urgent ? 8 : 14;
            int cropScore = recipientAcceptsCrop(recipient, listing.crop) ? 15 : 5;
            int donationScore;
            if (listing.donation) donationScore = recipient.acceptsDonations ? 15 : 2;
            else donationScore = 12;

            int total = Math.min(100, distanceScore + quantityScore + timingScore + cropScore + donationScore);
            String explanation = "Distance " + distanceScore + "/25 • quantity " + quantityScore +
                    "/25 • timing " + timingScore + "/20 • crop " + cropScore +
                    "/15 • listing fit " + donationScore + "/15";
            results.add(new MatchResult(recipient, total, distance, explanation));
        }
        results.sort(Comparator.comparingInt((MatchResult r) -> r.score).reversed());
        return results;
    }

    private boolean recipientAcceptsCrop(RecipientProfile recipient, String crop) {
        if (recipient.cropPreferences == null || recipient.cropPreferences.isBlank() ||
                recipient.cropPreferences.equalsIgnoreCase("all")) return true;
        String lowerCrop = crop.toLowerCase();
        for (String part : recipient.cropPreferences.toLowerCase().split(",")) {
            String token = part.trim();
            if (!token.isEmpty() && (lowerCrop.contains(token) || token.contains(lowerCrop))) return true;
        }
        return false;
    }

    private void optimizeCurrentRoute() {
        ProduceListing listing = selectedSmartMatchListing();
        if (listing == null) {
            JOptionPane.showMessageDialog(this, "Choose a listing first.");
            return;
        }
        List<MatchResult> matches = calculateMatches(listing);
        if (matches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one recipient profile first.");
            return;
        }

        ArrayList<RecipientProfile> selected = new ArrayList<>();
        double capacity = 0;
        for (MatchResult match : matches) {
            selected.add(match.recipient);
            capacity += match.recipient.capacityLbs;
            if (capacity >= listing.quantity) break;
            if (selected.size() >= 5) break;
        }

        Coordinate origin = coordinateForListing(listing);
        double baseline = routeDistance(origin, selected);
        ArrayList<RecipientProfile> remaining = new ArrayList<>(selected);
        ArrayList<RecipientProfile> optimized = new ArrayList<>();
        Coordinate current = origin;
        while (!remaining.isEmpty()) {
            RecipientProfile nearest = null;
            double best = Double.MAX_VALUE;
            for (RecipientProfile candidate : remaining) {
                double d = distanceMiles(current.lat, current.lon, candidate.lat, candidate.lon);
                if (d < best) {
                    best = d;
                    nearest = candidate;
                }
            }
            optimized.add(nearest);
            current = new Coordinate(nearest.lat, nearest.lon);
            remaining.remove(nearest);
        }

        double optimizedMiles = routeDistance(origin, optimized);
        double savings = baseline <= 0 ? 0 : Math.max(0, (baseline - optimizedMiles) / baseline * 100.0);
        lastMatchedListing = listing;
        lastOptimizedRoute = optimized;

        smartRouteSummaryLabel.setText(String.format(Locale.US,
                "Suggested route • %.1f mi estimated • %.0f%% shorter than match-order route",
                optimizedMiles, savings));
        StringBuilder route = new StringBuilder(currentUser.farmName);
        for (RecipientProfile recipient : optimized) route.append("  →  ").append(recipient.name);
        route.append("\nApproximate straight-line routing for prototype comparison, not turn-by-turn driving directions.");
        smartRouteDetailsArea.setText(route.toString());
    }

    private double routeDistance(Coordinate origin, List<RecipientProfile> route) {
        double miles = 0;
        double lat = origin.lat;
        double lon = origin.lon;
        for (RecipientProfile recipient : route) {
            miles += distanceMiles(lat, lon, recipient.lat, recipient.lon);
            lat = recipient.lat;
            lon = recipient.lon;
        }
        return miles;
    }

    private void showAddRecipientDialog() {
        JTextField name = styledTextField();
        JComboBox<String> type = styledCombo(new String[]{"Food pantry", "Community organization", "Restaurant / kitchen", "Family pickup", "Other"});
        JComboBox<String> area = styledCombo(new String[]{"Waller, TX", "Prairie View, TX", "Hempstead, TX"});
        JTextField capacity = styledTextField();
        capacity.setText("50");
        JTextField crops = styledTextField();
        crops.setText("all");
        JCheckBox today = new JCheckBox("Can pick up today");
        JCheckBox donations = new JCheckBox("Accepts free donations");
        today.setOpaque(false);
        donations.setOpaque(false);

        JPanel form = new JPanel();
        form.setBackground(SURFACE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(labeledField("Recipient or organization name", name));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Profile type", type));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Pickup area", area));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Maximum amount (lbs)", capacity));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Crop preferences (comma separated or all)", crops));
        form.add(Box.createVerticalStrut(8));
        form.add(today);
        form.add(donations);

        int result = JOptionPane.showConfirmDialog(this, form, "Add recipient profile",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double cap = Double.parseDouble(capacity.getText().trim());
            if (name.getText().trim().isEmpty() || cap <= 0) throw new NumberFormatException();
            Coordinate point = approximateCoordinateForLocation(String.valueOf(area.getSelectedItem()));
            RecipientProfile profile = new RecipientProfile(
                    data.nextRecipientId++, name.getText().trim(), String.valueOf(type.getSelectedItem()),
                    String.valueOf(area.getSelectedItem()), point.lat, point.lon, cap,
                    today.isSelected(), donations.isSelected(), crops.getText().trim(), false
            );
            data.recipients.add(profile);
            saveData();
            runSmartMatch();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a name and a capacity greater than zero.");
        }
    }

    private Coordinate approximateCoordinateForLocation(String location) {
        String lower = location == null ? "" : location.toLowerCase();
        if (lower.contains("prairie view")) return new Coordinate(30.0930, -95.9870);
        if (lower.contains("hempstead")) return new Coordinate(30.0974, -96.0783);
        if (lower.contains("waller")) return new Coordinate(30.0588, -95.9263);
        int hash = Math.abs(lower.hashCode());
        return new Coordinate(30.04 + (hash % 45) / 1000.0, -95.96 + ((hash / 71) % 60) / 1000.0);
    }

    private double distanceMiles(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMiles = 3958.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusMiles * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static class Coordinate {
        final double lat;
        final double lon;
        Coordinate(double lat, double lon) { this.lat = lat; this.lon = lon; }
    }

    private static class MatchResult {
        final RecipientProfile recipient;
        final int score;
        final double distanceMiles;
        final String explanation;
        MatchResult(RecipientProfile recipient, int score, double distanceMiles, String explanation) {
            this.recipient = recipient;
            this.score = score;
            this.distanceMiles = distanceMiles;
            this.explanation = explanation;
        }
    }


    // ============================================================
    // HARVESTSCAN • LOCAL COMPUTER-VISION PROTOTYPE
    // ============================================================

    private JPanel createHarvestScanPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));
        page.add(titleBlock(
                "HarvestScan",
                "Upload a produce photo and inspect visible color, darkness, and discoloration signals before deciding how quickly to distribute it."
        ), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);

        ShadowPanel uploadCard = new ShadowPanel(24, SURFACE);
        uploadCard.setBorder(new EmptyBorder(22, 22, 22, 22));
        uploadCard.setLayout(new BoxLayout(uploadCard, BoxLayout.Y_AXIS));

        uploadCard.add(smallLabel("STEP 1 • PHOTO", FOREST));
        uploadCard.add(Box.createVerticalStrut(6));
        uploadCard.add(cardTitle("Add a harvest image"));
        uploadCard.add(Box.createVerticalStrut(6));
        JTextArea scanIntro = textArea(
                "HarvestScan performs local pixel-feature analysis on the selected image. The photo stays on your computer and is not uploaded by this prototype.",
                FONT_BODY, MUTED
        );
        scanIntro.setRows(3);
        scanIntro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        uploadCard.add(scanIntro);
        uploadCard.add(Box.createVerticalStrut(14));

        RoundedPanel previewFrame = new RoundedPanel(20, new Color(239, 243, 239));
        previewFrame.setLayout(new BorderLayout());
        previewFrame.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewFrame.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        previewFrame.setPreferredSize(new Dimension(420, 310));
        scanPreviewLabel = new JLabel("Choose a JPG or PNG to begin", SwingConstants.CENTER);
        scanPreviewLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        scanPreviewLabel.setForeground(MUTED);
        previewFrame.add(scanPreviewLabel, BorderLayout.CENTER);
        uploadCard.add(previewFrame);
        uploadCard.add(Box.createVerticalStrut(14));

        JPanel photoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        photoActions.setOpaque(false);
        photoActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        RoundedButton choose = new RoundedButton("Choose photo", FOREST, Color.WHITE);
        RoundedButton analyze = new RoundedButton("Analyze visible quality", SAGE, FOREST_DARK);
        photoActions.add(choose);
        photoActions.add(Box.createHorizontalStrut(8));
        photoActions.add(analyze);
        uploadCard.add(photoActions);

        ShadowPanel resultCard = new ShadowPanel(24, SAGE_LIGHT);
        resultCard.setBorder(new EmptyBorder(22, 24, 22, 24));
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
        resultCard.add(smallLabel("STEP 2 • VISUAL SIGNAL", FOREST));
        resultCard.add(Box.createVerticalStrut(6));
        resultCard.add(cardTitle("Visible quality estimate"));
        resultCard.add(Box.createVerticalStrut(18));

        scanScoreLabel = new JLabel("No image analyzed");
        scanScoreLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
        scanScoreLabel.setForeground(TEXT);
        scanScoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.add(scanScoreLabel);
        resultCard.add(Box.createVerticalStrut(8));

        scanQualityBar = new JProgressBar(0, 100);
        scanQualityBar.setValue(0);
        scanQualityBar.setStringPainted(true);
        scanQualityBar.setString("Waiting for image");
        scanQualityBar.setForeground(FOREST);
        scanQualityBar.setBackground(new Color(222, 230, 223));
        scanQualityBar.setBorderPainted(false);
        scanQualityBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        scanQualityBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
        resultCard.add(scanQualityBar);
        resultCard.add(Box.createVerticalStrut(16));

        RoundedPanel features = new RoundedPanel(18, SURFACE);
        features.setBorder(new EmptyBorder(16, 18, 16, 18));
        features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
        features.setAlignmentX(Component.LEFT_ALIGNMENT);
        features.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        features.add(cardTitle("What the prototype measured"));
        features.add(Box.createVerticalStrut(8));
        scanDetailsArea = textArea(
                "After analysis, HarvestScan will show the measurements first, followed by one simple summary of what they mean together.",
                FONT_BODY, MUTED
        );
        scanDetailsArea.setRows(14);
        features.add(scanDetailsArea);
        resultCard.add(features);
        resultCard.add(Box.createVerticalStrut(16));

        RoundedPanel notice = new RoundedPanel(16, new Color(247, 244, 235));
        notice.setBorder(new EmptyBorder(13, 14, 13, 14));
        notice.setLayout(new BorderLayout());
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea noticeText = textArea(
                "Important: HarvestScan is not a disease detector or food-safety test. It is a transparent image-processing prototype. Never distribute produce that looks or smells unsafe.",
                11, new Color(103, 91, 70)
        );
        noticeText.setRows(3);
        notice.add(noticeText, BorderLayout.CENTER);
        resultCard.add(notice);
        resultCard.add(Box.createVerticalGlue());

        JLabel helperLink = new JLabel("The latest visual signal can add up to 18 points to Harvest Helper urgency.");
        helperLink.setFont(new Font("SansSerif", Font.BOLD, 11));
        helperLink.setForeground(FOREST_DARK);
        helperLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.add(helperLink);

        columns.add(uploadCard);
        columns.add(resultCard);
        page.add(columns, BorderLayout.CENTER);

        choose.addActionListener(e -> chooseHarvestScanImage());
        analyze.addActionListener(e -> analyzeCurrentHarvestImage());
        return page;
    }

    private void chooseHarvestScanImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a produce photo");
        chooser.setFileFilter(new FileNameExtensionFilter("Images (JPG, JPEG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) throw new IOException("Unsupported image");
            latestScanImage = image;
            latestScanFileName = file.getName();
            latestVisualRiskAdjustment = 0;

            int targetW = Math.max(320, scanPreviewLabel.getWidth() - 24);
            int targetH = Math.max(220, scanPreviewLabel.getHeight() - 24);
            double scale = Math.min((double) targetW / image.getWidth(), (double) targetH / image.getHeight());
            scale = Math.min(1.0, scale);
            int w = Math.max(1, (int) (image.getWidth() * scale));
            int h = Math.max(1, (int) (image.getHeight() * scale));
            Image scaled = image.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            scanPreviewLabel.setText("");
            scanPreviewLabel.setIcon(new ImageIcon(scaled));
            scanPreviewLabel.setToolTipText(file.getName());
            scanScoreLabel.setText("Image ready to analyze");
            scanScoreLabel.setForeground(TEXT);
            scanQualityBar.setValue(0);
            scanQualityBar.setString("Not analyzed");
            scanDetailsArea.setText("Selected " + file.getName() + ". Press Analyze visible quality to calculate image features.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "HarvestHub could not read that image.", "HarvestScan", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void analyzeCurrentHarvestImage() {
        if (latestScanImage == null) {
            JOptionPane.showMessageDialog(this, "Choose a produce photo first.");
            return;
        }

        BufferedImage image = latestScanImage;
        long samples = 0;
        double brightnessSum = 0;
        double brightnessSq = 0;
        long darkPixels = 0;
        long brownPixels = 0;
        long highlyDesaturated = 0;

        int step = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 240);
        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                double brightness = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
                brightnessSum += brightness;
                brightnessSq += brightness * brightness;
                samples++;

                if (brightness < 0.18) darkPixels++;
                if (r > 70 && r > g * 1.12 && g > b * 1.08 && brightness < 0.68) brownPixels++;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (max - min < 18 && brightness > 0.25 && brightness < 0.78) highlyDesaturated++;
            }
        }

        double avg = brightnessSum / Math.max(1, samples);
        double variance = Math.max(0, brightnessSq / Math.max(1, samples) - avg * avg);
        double darkRatio = darkPixels / (double) Math.max(1, samples);
        double brownRatio = brownPixels / (double) Math.max(1, samples);
        double dullRatio = highlyDesaturated / (double) Math.max(1, samples);

        int visualRisk = 0;
        visualRisk += (int) Math.round(Math.min(8, darkRatio * 45));
        visualRisk += (int) Math.round(Math.min(8, brownRatio * 50));
        visualRisk += (int) Math.round(Math.min(4, dullRatio * 20));
        if (avg < 0.28) visualRisk += 3;
        visualRisk = Math.max(0, Math.min(18, visualRisk));
        latestVisualRiskAdjustment = visualRisk;

        int quality = Math.max(0, Math.min(100, 92 - visualRisk * 4));
        scanQualityBar.setValue(quality);
        scanQualityBar.setString(quality + " / 100 visual quality");
        scanQualityBar.setForeground(quality >= 75 ? FOREST : quality >= 55 ? ACCENT.darker() : RESCUE);
        scanScoreLabel.setText(quality >= 75 ? "Low visible-risk signal" : quality >= 55 ? "Review this harvest soon" : "High visible-risk signal");
        scanScoreLabel.setForeground(quality >= 75 ? FOREST : quality >= 55 ? ACCENT.darker() : RESCUE);

        String variation = variance < 0.015 ? "Low" : variance < 0.045 ? "Moderate" : "High";
        String estimate = quality >= 75
                ? "Low visible-risk signal"
                : quality >= 55
                ? "Review this harvest soon"
                : "High visible-risk signal";

        String summary = buildHarvestScanSummary(
                quality,
                darkRatio,
                brownRatio,
                dullRatio,
                variation,
                latestVisualRiskAdjustment
        );

        scanDetailsArea.setText(String.format(Locale.US,
                "File: %s\n\n" +
                        "Average brightness: %.0f%%\n\n" +
                        "Dark-area ratio: %.1f%%\n\n" +
                        "Brown/discoloration signal: %.1f%%\n\n" +
                        "Low-saturation signal: %.1f%%\n\n" +
                        "Color/light variation: %s\n\n" +
                        "Overall summary\n%s",
                latestScanFileName,
                avg * 100,
                darkRatio * 100,
                brownRatio * 100,
                dullRatio * 100,
                variation,
                summary));
    }

    private String buildHarvestScanSummary(
            int quality,
            double darkRatio,
            double brownRatio,
            double dullRatio,
            String variation,
            int urgencyPoints) {

        ArrayList<String> observations = new ArrayList<>();

        if (brownRatio >= 0.12) {
            observations.add("a stronger brown or discoloration signal");
        } else if (brownRatio >= 0.04) {
            observations.add("some visible brown or discoloration areas");
        }

        if (darkRatio >= 0.12) {
            observations.add("a larger amount of very dark coloring");
        } else if (darkRatio >= 0.04) {
            observations.add("some darker areas");
        }

        if (dullRatio >= 0.16) {
            observations.add("a noticeable amount of dull or faded color");
        } else if (dullRatio >= 0.06) {
            observations.add("some muted coloring");
        }

        if ("High".equals(variation)) {
            observations.add("a lot of variation in color and lighting");
        } else if ("Moderate".equals(variation) && observations.isEmpty()) {
            observations.add("some variation in color and lighting");
        }

        String evidence;
        if (observations.isEmpty()) {
            evidence = "the image shows relatively few visible warning signals";
        } else if (observations.size() == 1) {
            evidence = "the image shows " + observations.get(0);
        } else {
            StringBuilder joined = new StringBuilder("the image shows ");
            for (int i = 0; i < observations.size(); i++) {
                if (i > 0) joined.append(i == observations.size() - 1 ? " and " : ", ");
                joined.append(observations.get(i));
            }
            evidence = joined.toString();
        }

        String recommendation;
        if (quality >= 75) {
            recommendation = "HarvestScan found a low visible-risk signal, so the photo does not suggest that this harvest needs immediate attention based on appearance alone.";
        } else if (quality >= 55) {
            recommendation = "HarvestScan recommends reviewing this harvest soon and checking its condition before deciding how quickly it should be distributed.";
        } else {
            recommendation = "HarvestScan found a high visible-risk signal, so this harvest should be checked carefully and handled promptly if it is still appropriate to distribute.";
        }

        String helperEffect = urgencyPoints <= 3
                ? "The image adds little or no extra urgency to Harvest Helper."
                : "Because of these visible signals, Harvest Helper adds " + urgencyPoints + " urgency points to its overall recommendation.";

        return "According to what HarvestScan can see, " + evidence + ". " +
                recommendation + " " + helperEffect +
                " The final decision should still consider the produce's age, storage conditions, weather, smell, and how it looks in person.";
    }

    // ============================================================
    // FAVORITES
    // ============================================================

    private JPanel createFavoritesPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));
        page.add(titleBlock("Favorites", "Saved produce you may want to come back to."), BorderLayout.NORTH);

        favoritesGrid = new JPanel(new GridLayout(0, 2, 14, 14));
        favoritesGrid.setOpaque(false);
        ViewportWidthPanel wrapper = new ViewportWidthPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(favoritesGrid, BorderLayout.NORTH);
        page.add(cleanScroll(wrapper), BorderLayout.CENTER);
        refreshFavorites();
        return page;
    }

    private void refreshFavorites() {
        if (favoritesGrid == null || currentUser == null || currentUser.isFarmer()) return;
        favoritesGrid.removeAll();
        int count = 0;
        for (ProduceListing listing : data.listings) {
            if (currentUser.favoriteListingIds.contains(listing.id)) {
                favoritesGrid.add(createProduceCard(listing));
                count++;
            }
        }
        if (count == 0) favoritesGrid.add(emptyState("Nothing saved yet", "Use the Save button in Marketplace to keep listings here."));
        favoritesGrid.revalidate();
        favoritesGrid.repaint();
    }

    // ============================================================
    // FARMER DASHBOARD
    // ============================================================

    private JPanel createFarmerPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleBlock("My Farm", currentUser.farmName + " • Manage your current harvest listings."), BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        buttons.setOpaque(false);
        RoundedButton helper = new RoundedButton("Harvest Helper", SAGE, FOREST_DARK);
        helper.addActionListener(e -> showPage("Harvest Helper"));
        RoundedButton normal = new RoundedButton("+ Post produce", FOREST, Color.WHITE);
        normal.addActionListener(e -> showListingDialog(false));
        RoundedButton rescue = new RoundedButton("Rescue My Harvest", RESCUE, Color.WHITE);
        rescue.addActionListener(e -> showListingDialog(true));
        buttons.add(helper);
        buttons.add(normal);
        buttons.add(rescue);
        header.add(buttons, BorderLayout.EAST);

        myListingsPanel = new ViewportWidthPanel();
        myListingsPanel.setOpaque(false);
        myListingsPanel.setLayout(new BoxLayout(myListingsPanel, BoxLayout.Y_AXIS));

        page.add(header, BorderLayout.NORTH);
        page.add(cleanScroll(myListingsPanel), BorderLayout.CENTER);
        refreshMyListings();
        return page;
    }

    private void refreshMyListings() {
        if (myListingsPanel == null || currentUser == null || !currentUser.isFarmer()) return;
        myListingsPanel.removeAll();
        int count = 0;
        for (ProduceListing listing : data.listings) {
            if (listing.ownerUserId == currentUser.id) {
                myListingsPanel.add(createFarmerListingCard(listing));
                myListingsPanel.add(Box.createVerticalStrut(12));
                count++;
            }
        }
        if (count == 0) myListingsPanel.add(emptyState("No active listings", "Post produce when you have extra harvest available."));
        myListingsPanel.revalidate();
        myListingsPanel.repaint();
    }

    private JPanel createFarmerListingCard(ProduceListing listing) {
        ShadowPanel card = new ShadowPanel(20, SURFACE);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(15, 17, 15, 17));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        ProduceIllustrationPanel image = new ProduceIllustrationPanel(listing.crop);
        image.setPreferredSize(new Dimension(88, 88));

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel(listing.crop);
        title.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.add(title);
        if (listing.urgent) titleRow.add(pill("RESCUE", SOFT_RED, RESCUE));
        if (listing.donation) titleRow.add(pill("FREE", SOFT_GREEN, FOREST_DARK));

        JLabel info = new JLabel(formatNumber(listing.quantity) + " lbs  •  " +
                (listing.donation ? "Free" : String.format("$%.2f/lb", listing.price)) + "  •  Pickup by " + listing.deadline);
        info.setFont(new Font("SansSerif", Font.PLAIN, 13));
        info.setForeground(MUTED);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel location = new JLabel(listing.location);
        location.setFont(new Font("SansSerif", Font.BOLD, 12));
        location.setForeground(FOREST);
        location.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(titleRow);
        center.add(Box.createVerticalStrut(7));
        center.add(info);
        center.add(Box.createVerticalStrut(6));
        center.add(location);

        RoundedButton remove = new RoundedButton("Remove", new Color(242, 243, 242), TEXT);
        remove.setCompact(true);
        remove.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Remove this listing?", "Remove listing", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                data.listings.remove(listing);
                saveData();
                refreshAllDynamicPanels();
            }
        });

        JPanel action = new JPanel(new GridBagLayout());
        action.setOpaque(false);
        action.add(remove);

        card.add(image, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        return card;
    }

    private void showListingDialog(boolean rescueMode) {
        JTextField cropField = styledTextField();
        JTextField quantityField = styledTextField();
        JTextField priceField = styledTextField();
        JTextField locationField = styledTextField();
        JTextField deadlineField = styledTextField();
        JTextField noteField = styledTextField();

        if (currentUser != null && currentUser.isFarmer() && currentUser.defaultPickupLocation
                && currentUser.streetAddress != null && !currentUser.streetAddress.isBlank()) {
            locationField.setText(privateAddressForUser(currentUser));
        } else if (currentUser != null) {
            locationField.setText(publicLocationForUser(currentUser));
        } else {
            locationField.setText("Waller, TX");
        }
        deadlineField.setText(LocalDate.now().plusDays(rescueMode ? 1 : 3).toString());

        JCheckBox donationBox = new JCheckBox("Make this a free donation");
        donationBox.setOpaque(false);
        if (rescueMode) donationBox.setSelected(true);
        priceField.setText(rescueMode ? "0" : "");
        priceField.setEnabled(!rescueMode);

        donationBox.addActionListener(e -> {
            boolean donation = donationBox.isSelected();
            priceField.setEnabled(!donation);
            if (donation) priceField.setText("0");
            else if (priceField.getText().trim().equals("0")) priceField.setText("");
        });

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(SURFACE);
        form.setBorder(new EmptyBorder(7, 7, 4, 7));

        if (rescueMode) {
            JTextArea alert = textArea("Rescue listings are highlighted so nearby users know the food needs to move quickly.", 12, RESCUE);
            form.add(alert);
            form.add(Box.createVerticalStrut(10));
        }

        form.add(labeledField("Produce", cropField));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Quantity (lbs)", quantityField));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Price per lb", priceField));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Pickup address / location (exact address stays private until reservation)", locationField));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Pickup deadline (YYYY-MM-DD)", deadlineField));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Short note", noteField));
        form.add(Box.createVerticalStrut(8));
        form.add(donationBox);

        int result = JOptionPane.showConfirmDialog(this, form,
                rescueMode ? "Rescue My Harvest" : "Post Produce",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String crop = cropField.getText().trim();
            double quantity = Double.parseDouble(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            String location = locationField.getText().trim();
            String deadline = deadlineField.getText().trim();
            String note = noteField.getText().trim();

            if (crop.isEmpty() || location.isEmpty() || deadline.isEmpty() || quantity <= 0 || price < 0) {
                JOptionPane.showMessageDialog(this, "Please complete the listing with valid values.");
                return;
            }

            ProduceListing listing = new ProduceListing(
                    data.nextListingId++, currentUser.id, crop, currentUser.farmName,
                    quantity, donationBox.isSelected() ? 0 : price, location, deadline,
                    donationBox.isSelected(), rescueMode, note
            );
            data.listings.add(0, listing);
            saveData();
            refreshAllDynamicPanels();
            JOptionPane.showMessageDialog(this,
                    rescueMode ? "Rescue alert posted!" : "Your produce is now listed.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity and price must be valid numbers.");
        }
    }

    // ============================================================
    // COMMUNITY MAP
    // ============================================================

    private JPanel createMapPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));

        JPanel heading = titleBlock(
                "Community Map",
                "Explore HarvestHub pickup areas near Waller High School. Drag to pan, scroll to zoom, and click a farm pin for details."
        );
        page.add(heading, BorderLayout.NORTH);

        ShadowPanel mapCard = new ShadowPanel(24, SURFACE);
        mapCard.setLayout(new BorderLayout());
        mapCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        communityMapPanel = new InteractiveMapPanel();
        communityMapPanel.setPreferredSize(new Dimension(900, 600));
        mapCard.add(communityMapPanel, BorderLayout.CENTER);

        page.add(mapCard, BorderLayout.CENTER);
        return page;
    }

    /**
     * A real, draggable slippy-map built with standard Java/Swing.
     *
     * It uses OpenStreetMap raster tiles, stores downloaded tiles on disk,
     * and geocodes only the small number of HarvestHub locations needed by
     * this student prototype. Search requests are manual (button/Enter), not
     * autocomplete, and geocoding is rate-limited to one request per second.
     */
    private class InteractiveMapPanel extends JPanel {
        private static final int TILE_SIZE = 256;
        private static final int MIN_ZOOM = 10;
        private static final int MAX_ZOOM = 18;
        private static final long TILE_CACHE_MS = 7L * 24 * 60 * 60 * 1000;
        private static final String SCHOOL_ADDRESS = "31213 Waller Spring Creek Rd, Waller, TX 77484";
        private static final String MAP_USER_AGENT = "HarvestHubStudentProject/1.0 (Waller-TX-school-project)";

        // Waller city center is only a startup fallback while the school address loads.
        private double centerLat = 30.058752;
        private double centerLon = -95.926336;
        private int zoom = 13;

        private Point dragAnchor;
        private double dragCenterWorldX;
        private double dragCenterWorldY;
        private boolean dragged;

        private final Map<String, BufferedImage> tileMemoryCache = new ConcurrentHashMap<>();
        private final java.util.Set<String> tileLoading = ConcurrentHashMap.newKeySet();
        private final ExecutorService tileExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "harvesthub-map-tile");
            t.setDaemon(true);
            return t;
        });
        private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "harvesthub-map-geocoder");
            t.setDaemon(true);
            return t;
        });

        private final File tileCacheDir = new File("harvesthub-map-cache");
        private final File geocodeCacheFile = new File("harvesthub-geocode-cache.properties");
        private final Properties geocodeDiskCache = new Properties();
        private long lastGeocodeRequestAt = 0L;

        private final ArrayList<MapMarker> farmMarkers = new ArrayList<>();
        private final Map<MapMarker, Point> markerScreenPoints = new LinkedHashMap<>();
        private final ArrayList<GeoPoint> smartRoutePoints = new ArrayList<>();
        private final ArrayList<String> smartRouteLabels = new ArrayList<>();
        private GeoPoint schoolPoint;
        private GeoPoint searchPoint;
        private String searchPointLabel = "";

        private final ShadowPanel searchBox = new ShadowPanel(18, SURFACE);
        private final JTextField searchField = new JTextField(SCHOOL_ADDRESS);
        private final RoundedButton searchButton = new RoundedButton("Search", FOREST, Color.WHITE);
        private final RoundedButton schoolButton = new RoundedButton("⌂  Waller High School", SURFACE, FOREST_DARK);
        private final RoundedButton zoomInButton = new RoundedButton("+", SURFACE, TEXT);
        private final RoundedButton zoomOutButton = new RoundedButton("−", SURFACE, TEXT);
        private final RoundedButton closeDetailsButton = new RoundedButton("×", new Color(245, 246, 244), MUTED);

        private final ShadowPanel detailCard = new ShadowPanel(20, SURFACE);
        private final JLabel detailEyebrow = new JLabel("FARM");
        private final JLabel detailTitle = new JLabel();
        private final JLabel detailMeta = new JLabel();
        private final JTextArea detailBody = new JTextArea();

        private final JLabel statusLabel = new JLabel("Loading local map…");
        private final JLabel attributionLabel = new JLabel("© OpenStreetMap contributors");
        private volatile boolean tileNetworkIssue = false;

        InteractiveMapPanel() {
            setLayout(null);
            setBackground(new Color(232, 235, 231));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);

            tileCacheDir.mkdirs();
            loadGeocodeCache();
            buildMapControls();
            installMapInteractions();
            reloadFarmMarkers();
            locateSchool();
        }

        private void buildMapControls() {
            searchBox.setLayout(new BorderLayout(8, 0));
            searchBox.setBorder(new EmptyBorder(7, 9, 7, 7));
            searchField.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
            searchField.setForeground(TEXT);
            searchField.setBorder(new EmptyBorder(6, 8, 6, 8));
            searchField.setOpaque(false);
            searchField.setToolTipText("Search only when you press Search or Enter — no autocomplete requests are sent.");
            searchButton.setFont(new Font("SansSerif", Font.BOLD, 12));
            searchBox.add(searchField, BorderLayout.CENTER);
            searchBox.add(searchButton, BorderLayout.EAST);
            add(searchBox);

            schoolButton.setFont(new Font("SansSerif", Font.BOLD, 12));
            schoolButton.setToolTipText(SCHOOL_ADDRESS);
            add(schoolButton);

            zoomInButton.setFont(new Font("SansSerif", Font.BOLD, 22));
            zoomOutButton.setFont(new Font("SansSerif", Font.BOLD, 22));
            add(zoomInButton);
            add(zoomOutButton);

            statusLabel.setOpaque(true);
            statusLabel.setBackground(new Color(255, 255, 255, 225));
            statusLabel.setForeground(MUTED);
            statusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            statusLabel.setBorder(new EmptyBorder(6, 9, 6, 9));
            add(statusLabel);

            attributionLabel.setOpaque(true);
            attributionLabel.setBackground(new Color(255, 255, 255, 215));
            attributionLabel.setForeground(new Color(74, 82, 77));
            attributionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            attributionLabel.setBorder(new EmptyBorder(3, 6, 3, 6));
            add(attributionLabel);

            detailCard.setLayout(null);
            detailCard.setBorder(new EmptyBorder(0, 0, 0, 0));
            detailCard.setVisible(false);

            detailEyebrow.setFont(new Font("SansSerif", Font.BOLD, FONT_EYEBROW));
            detailEyebrow.setForeground(FOREST);
            detailTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
            detailTitle.setForeground(TEXT);
            detailMeta.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
            detailMeta.setForeground(MUTED);

            detailBody.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
            detailBody.setForeground(TEXT);
            detailBody.setOpaque(false);
            detailBody.setEditable(false);
            detailBody.setLineWrap(true);
            detailBody.setWrapStyleWord(true);
            detailBody.setFocusable(false);

            closeDetailsButton.setFont(new Font("SansSerif", Font.BOLD, 15));
            closeDetailsButton.addActionListener(e -> detailCard.setVisible(false));

            detailCard.add(detailEyebrow);
            detailCard.add(detailTitle);
            detailCard.add(detailMeta);
            detailCard.add(detailBody);
            detailCard.add(closeDetailsButton);
            add(detailCard);

            searchButton.addActionListener(e -> searchMap());
            searchField.addActionListener(e -> searchMap());
            schoolButton.addActionListener(e -> centerOnSchool());
            zoomInButton.addActionListener(e -> changeZoom(1));
            zoomOutButton.addActionListener(e -> changeZoom(-1));
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();

            int searchWidth = Math.min(470, Math.max(300, w - 190));
            searchBox.setBounds(18, 18, searchWidth, 48);
            schoolButton.setBounds(18, 76, 188, 38);

            zoomInButton.setBounds(w - 58, 18, 40, 40);
            zoomOutButton.setBounds(w - 58, 62, 40, 40);

            int statusWidth = Math.min(250, Math.max(150, w / 4));
            statusLabel.setBounds(w - statusWidth - 18, 112, statusWidth, 30);

            int attrWidth = 165;
            attributionLabel.setBounds(w - attrWidth - 10, h - 24, attrWidth, 20);

            int detailWidth = Math.min(430, Math.max(310, w - 100));
            int detailHeight = 146;
            detailCard.setBounds(18, Math.max(150, h - detailHeight - 18), detailWidth, detailHeight);
            detailEyebrow.setBounds(18, 14, detailWidth - 60, 16);
            detailTitle.setBounds(18, 31, detailWidth - 64, 26);
            detailMeta.setBounds(18, 58, detailWidth - 36, 20);
            detailBody.setBounds(18, 80, detailWidth - 36, 52);
            closeDetailsButton.setBounds(detailWidth - 43, 12, 28, 28);
        }

        private void installMapInteractions() {
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        requestFocusInWindow();
                        dragAnchor = e.getPoint();
                        dragCenterWorldX = lonToWorldX(centerLon, zoom);
                        dragCenterWorldY = latToWorldY(centerLat, zoom);
                        dragged = false;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragAnchor == null) return;
                    int dx = e.getX() - dragAnchor.x;
                    int dy = e.getY() - dragAnchor.y;
                    if (Math.abs(dx) + Math.abs(dy) > 3) dragged = true;

                    double newWorldX = dragCenterWorldX - dx;
                    double newWorldY = dragCenterWorldY - dy;
                    centerLon = worldXToLon(newWorldX, zoom);
                    centerLat = worldYToLat(newWorldY, zoom);
                    clampCenter();
                    detailCard.setVisible(false);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    dragAnchor = null;
                    if (!dragged && SwingUtilities.isLeftMouseButton(e)) {
                        selectMarkerAt(e.getPoint());
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        zoomAroundPoint(1, e.getPoint());
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int direction = e.getWheelRotation() < 0 ? 1 : -1;
                    zoomAroundPoint(direction, e.getPoint());
                }
            };

            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        void reloadFarmMarkers() {
            farmMarkers.clear();

            LinkedHashMap<String, MapMarker> grouped = new LinkedHashMap<>();
            for (ProduceListing listing : data.listings) {
                String key = listing.farmName + "|" + listing.location;
                MapMarker marker = grouped.get(key);
                if (marker == null) {
                    marker = new MapMarker(listing.farmName, listing.location, displayLocationForListing(listing));
                    grouped.put(key, marker);
                }
                marker.addListing(listing);
            }
            farmMarkers.addAll(grouped.values());

            // Only unique location strings are geocoded. Results are cached on disk.
            LinkedHashMap<String, ArrayList<MapMarker>> byLocation = new LinkedHashMap<>();
            for (MapMarker marker : farmMarkers) {
                GeoPoint builtIn = knownAreaPoint(marker.location);
                if (builtIn != null && !looksLikeStreetAddress(marker.location)) {
                    marker.point = builtIn;
                    marker.approximate = true;
                    continue;
                }
                byLocation.computeIfAbsent(marker.location, k -> new ArrayList<>()).add(marker);
            }

            for (Map.Entry<String, ArrayList<MapMarker>> entry : byLocation.entrySet()) {
                String location = entry.getKey();
                geocode(location, point -> {
                    if (point != null) {
                        for (MapMarker marker : entry.getValue()) {
                            marker.point = point;
                            marker.approximate = !looksLikeStreetAddress(marker.location);
                        }
                        repaint();
                    }
                }, false);
            }
            repaint();
        }


        void showSmartRoute(ProduceListing listing, List<RecipientProfile> route) {
            smartRoutePoints.clear();
            smartRouteLabels.clear();
            if (listing == null || route == null || route.isEmpty()) {
                repaint();
                return;
            }

            Coordinate listingCoordinate = coordinateForListing(listing);
            GeoPoint origin = new GeoPoint(listingCoordinate.lat, listingCoordinate.lon);
            smartRoutePoints.add(origin);
            smartRouteLabels.add(listing.farmName);
            double latSum = origin.lat;
            double lonSum = origin.lon;

            for (RecipientProfile recipient : route) {
                GeoPoint point = new GeoPoint(recipient.lat, recipient.lon);
                smartRoutePoints.add(point);
                smartRouteLabels.add(recipient.name);
                latSum += point.lat;
                lonSum += point.lon;
            }

            centerLat = latSum / smartRoutePoints.size();
            centerLon = lonSum / smartRoutePoints.size();
            zoom = smartRoutePoints.size() <= 2 ? 13 : 12;
            statusLabel.setText("Smart Match route • drag or zoom to explore");
            detailCard.setVisible(false);
            repaint();
        }

        private void locateSchool() {
            statusLabel.setText("Centering on Waller High School…");
            geocode(SCHOOL_ADDRESS, point -> {
                if (point != null) {
                    schoolPoint = point;
                    centerLat = point.lat;
                    centerLon = point.lon;
                    zoom = 14;
                    statusLabel.setText("Waller High School • zoom " + zoom);
                } else {
                    statusLabel.setText("Waller area • school search unavailable");
                }
                repaint();
            }, false);
        }

        private void centerOnSchool() {
            if (schoolPoint != null) {
                centerLat = schoolPoint.lat;
                centerLon = schoolPoint.lon;
                zoom = 14;
                statusLabel.setText("Waller High School • zoom " + zoom);
                showSchoolDetails();
                repaint();
            } else {
                locateSchool();
            }
        }

        private void searchMap() {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(Main.this, "Enter a place or address to search.");
                return;
            }

            statusLabel.setText("Searching…");
            geocode(query, point -> {
                if (point == null) {
                    statusLabel.setText("No location found");
                    JOptionPane.showMessageDialog(Main.this,
                            "I could not find that location. Try a more complete address.",
                            "Map search", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                searchPoint = point;
                searchPointLabel = query;
                centerLat = point.lat;
                centerLon = point.lon;
                zoom = Math.max(zoom, 15);
                statusLabel.setText("Search result • zoom " + zoom);
                showSearchDetails(query);
                repaint();
            }, true);
        }

        private void changeZoom(int amount) {
            int newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + amount));
            if (newZoom == zoom) return;
            zoom = newZoom;
            statusLabel.setText("Waller area • zoom " + zoom);
            detailCard.setVisible(false);
            repaint();
        }

        private void zoomAroundPoint(int amount, Point screenPoint) {
            int newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + amount));
            if (newZoom == zoom) return;

            GeoPoint underMouse = screenToGeo(screenPoint.x, screenPoint.y);
            zoom = newZoom;

            double targetX = lonToWorldX(underMouse.lon, zoom);
            double targetY = latToWorldY(underMouse.lat, zoom);
            double centerX = targetX - (screenPoint.x - getWidth() / 2.0);
            double centerY = targetY - (screenPoint.y - getHeight() / 2.0);

            centerLon = worldXToLon(centerX, zoom);
            centerLat = worldYToLat(centerY, zoom);
            clampCenter();
            statusLabel.setText("Waller area • zoom " + zoom);
            detailCard.setVisible(false);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            drawMapTiles(g2);
            drawSmartRoute(g2);
            drawMarkers(g2);

            if (tileNetworkIssue && tileMemoryCache.isEmpty()) {
                drawOfflineMessage(g2);
            }

            g2.dispose();
        }

        private void drawMapTiles(Graphics2D g2) {
            int w = getWidth();
            int h = getHeight();
            double centerWorldX = lonToWorldX(centerLon, zoom);
            double centerWorldY = latToWorldY(centerLat, zoom);

            int startTileX = (int) Math.floor((centerWorldX - w / 2.0) / TILE_SIZE);
            int endTileX = (int) Math.floor((centerWorldX + w / 2.0) / TILE_SIZE);
            int startTileY = (int) Math.floor((centerWorldY - h / 2.0) / TILE_SIZE);
            int endTileY = (int) Math.floor((centerWorldY + h / 2.0) / TILE_SIZE);
            int tileCount = 1 << zoom;

            g2.setColor(new Color(231, 235, 230));
            g2.fillRect(0, 0, w, h);

            for (int tx = startTileX; tx <= endTileX; tx++) {
                int wrappedX = ((tx % tileCount) + tileCount) % tileCount;
                for (int ty = startTileY; ty <= endTileY; ty++) {
                    if (ty < 0 || ty >= tileCount) continue;

                    int screenX = (int) Math.round(tx * TILE_SIZE - centerWorldX + w / 2.0);
                    int screenY = (int) Math.round(ty * TILE_SIZE - centerWorldY + h / 2.0);
                    String key = zoom + "_" + wrappedX + "_" + ty;
                    BufferedImage tile = getCachedTile(key, zoom, wrappedX, ty);

                    if (tile != null) {
                        g2.drawImage(tile, screenX, screenY, TILE_SIZE, TILE_SIZE, null);
                    } else {
                        g2.setColor(new Color(238, 241, 237));
                        g2.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        g2.setColor(new Color(224, 228, 223));
                        g2.drawRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }

        private BufferedImage getCachedTile(String key, int z, int x, int y) {
            BufferedImage memory = tileMemoryCache.get(key);
            if (memory != null) return memory;

            File file = new File(tileCacheDir, key + ".png");
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < TILE_CACHE_MS) {
                try {
                    BufferedImage disk = ImageIO.read(file);
                    if (disk != null) {
                        tileMemoryCache.put(key, disk);
                        return disk;
                    }
                } catch (IOException ignored) {
                }
            }

            requestTile(key, z, x, y, file);
            return null;
        }

        private void requestTile(String key, int z, int x, int y, File file) {
            if (!tileLoading.add(key)) return;

            tileExecutor.submit(() -> {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("https://tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(7000);
                    connection.setRequestProperty("User-Agent", MAP_USER_AGENT);
                    connection.setRequestProperty("Accept", "image/png");

                    if (connection.getResponseCode() == 200) {
                        try (InputStream in = connection.getInputStream()) {
                            BufferedImage image = ImageIO.read(in);
                            if (image != null) {
                                tileMemoryCache.put(key, image);
                                try {
                                    ImageIO.write(image, "png", file);
                                } catch (IOException ignored) {
                                }
                                tileNetworkIssue = false;
                            }
                        }
                    } else {
                        tileNetworkIssue = true;
                    }
                } catch (IOException ex) {
                    tileNetworkIssue = true;
                } finally {
                    if (connection != null) connection.disconnect();
                    tileLoading.remove(key);
                    SwingUtilities.invokeLater(this::repaint);
                }
            });
        }


        private void drawSmartRoute(Graphics2D g2) {
            if (smartRoutePoints.size() < 2) return;

            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(35, 92, 62, 190));
            Point previous = null;
            int index = 0;
            for (GeoPoint point : smartRoutePoints) {
                Point screen = geoToScreen(point.lat, point.lon);
                if (previous != null) g2.drawLine(previous.x, previous.y, screen.x, screen.y);

                g2.setColor(index == 0 ? BLUE_GRAY : FOREST);
                g2.fillOval(screen.x - 11, screen.y - 11, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                String number = index == 0 ? "F" : String.valueOf(index);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(number, screen.x - fm.stringWidth(number) / 2, screen.y + 4);

                if (index < smartRouteLabels.size()) {
                    String label = smartRouteLabels.get(index);
                    if (label.length() > 24) label = label.substring(0, 22) + "…";
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    int tw = g2.getFontMetrics().stringWidth(label);
                    g2.setColor(new Color(255, 255, 255, 235));
                    g2.fillRoundRect(screen.x - tw / 2 - 6, screen.y + 14, tw + 12, 21, 10, 10);
                    g2.setColor(TEXT);
                    g2.drawString(label, screen.x - tw / 2, screen.y + 28);
                }

                g2.setColor(new Color(35, 92, 62, 190));
                previous = screen;
                index++;
            }
        }

        private void drawMarkers(Graphics2D g2) {
            markerScreenPoints.clear();

            if (schoolPoint != null) {
                Point schoolScreen = geoToScreen(schoolPoint.lat, schoolPoint.lon);
                drawSchoolPin(g2, schoolScreen.x, schoolScreen.y);
            }

            if (searchPoint != null) {
                Point searchScreen = geoToScreen(searchPoint.lat, searchPoint.lon);
                drawSearchPin(g2, searchScreen.x, searchScreen.y);
            }

            int markerIndex = 0;
            for (MapMarker marker : farmMarkers) {
                if (marker.point == null) continue;
                Point p = geoToScreen(marker.point.lat, marker.point.lon);

                // If a listing only names a city (rather than a street address), spread
                // overlapping demo pins slightly on screen and label them as approximate.
                if (marker.approximate) {
                    int hash = Math.abs(marker.farmName.hashCode());
                    double angle = (hash % 360) * Math.PI / 180.0;
                    int radius = 18 + (markerIndex % 3) * 8;
                    p = new Point(p.x + (int) (Math.cos(angle) * radius),
                            p.y + (int) (Math.sin(angle) * radius));
                }
                markerIndex++;

                markerScreenPoints.put(marker, p);
                drawFarmPin(g2, p.x, p.y, marker);
            }
        }

        private void drawFarmPin(Graphics2D g2, int x, int y, MapMarker marker) {
            if (x < -50 || x > getWidth() + 50 || y < -50 || y > getHeight() + 50) return;

            Color fill = marker.urgent ? RESCUE : FOREST;
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(x - 15, y - 9, 30, 18);

            Polygon pin = new Polygon();
            pin.addPoint(x, y + 17);
            pin.addPoint(x - 13, y - 4);
            pin.addPoint(x + 13, y - 4);
            g2.setColor(fill);
            g2.fillPolygon(pin);
            g2.fillOval(x - 13, y - 17, 26, 26);
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 4, y - 8, 8, 8);

            String label = marker.farmName.length() > 24
                    ? marker.farmName.substring(0, 22) + "…"
                    : marker.farmName;
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            int textW = g2.getFontMetrics().stringWidth(label);
            int labelX = x - textW / 2 - 7;
            int labelY = y + 21;
            g2.setColor(new Color(255, 255, 255, 238));
            g2.fillRoundRect(labelX, labelY, textW + 14, 24, 12, 12);
            g2.setColor(TEXT);
            g2.drawString(label, x - textW / 2, labelY + 16);
        }

        private void drawSchoolPin(Graphics2D g2, int x, int y) {
            if (x < -50 || x > getWidth() + 50 || y < -50 || y > getHeight() + 50) return;

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(x - 17, y - 9, 34, 20);
            g2.setColor(BLUE_GRAY);
            g2.fillOval(x - 15, y - 15, 30, 30);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            String text = "H";
            g2.drawString(text, x - fm.stringWidth(text) / 2, y + 5);

            String label = "Waller High School";
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            int tw = g2.getFontMetrics().stringWidth(label);
            g2.setColor(new Color(255, 255, 255, 240));
            g2.fillRoundRect(x - tw / 2 - 8, y + 19, tw + 16, 24, 12, 12);
            g2.setColor(TEXT);
            g2.drawString(label, x - tw / 2, y + 35);
        }

        private void drawSearchPin(Graphics2D g2, int x, int y) {
            if (x < -50 || x > getWidth() + 50 || y < -50 || y > getHeight() + 50) return;
            g2.setColor(ACCENT);
            g2.fillOval(x - 10, y - 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.fillOval(x - 3, y - 3, 6, 6);
        }

        private void drawOfflineMessage(Graphics2D g2) {
            String line1 = "Map tiles could not load.";
            String line2 = "Check your internet connection. HarvestHub will retry automatically.";
            int boxW = 390;
            int boxH = 82;
            int x = Math.max(18, (getWidth() - boxW) / 2);
            int y = Math.max(150, (getHeight() - boxH) / 2);
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fillRoundRect(x, y, boxW, boxH, 20, 20);
            g2.setColor(TEXT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.drawString(line1, x + 18, y + 30);
            g2.setColor(MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString(line2, x + 18, y + 54);
        }

        private void selectMarkerAt(Point click) {
            if (schoolPoint != null) {
                Point p = geoToScreen(schoolPoint.lat, schoolPoint.lon);
                if (p.distance(click) <= 22) {
                    showSchoolDetails();
                    return;
                }
            }

            if (searchPoint != null) {
                Point p = geoToScreen(searchPoint.lat, searchPoint.lon);
                if (p.distance(click) <= 18) {
                    showSearchDetails(searchPointLabel);
                    return;
                }
            }

            MapMarker closest = null;
            double best = 28;
            for (Map.Entry<MapMarker, Point> entry : markerScreenPoints.entrySet()) {
                double distance = entry.getValue().distance(click);
                if (distance < best) {
                    best = distance;
                    closest = entry.getKey();
                }
            }

            if (closest != null) showFarmDetails(closest);
            else detailCard.setVisible(false);
        }

        private void showFarmDetails(MapMarker marker) {
            detailEyebrow.setText(marker.urgent ? "RESCUE LISTING" : "LOCAL FARM");
            detailEyebrow.setForeground(marker.urgent ? RESCUE : FOREST);
            detailTitle.setText(marker.farmName);

            String locationText = marker.publicLocation + (marker.approximate ? " • approximate map area" : " • exact pin, address private");
            detailMeta.setText(locationText);

            String crops = marker.cropSummary();
            String qty = formatNumber(marker.totalQuantity) + " lbs available";
            String price = marker.allFree ? "Free donation" : marker.hasFree ? "Free + paid listings" : "For sale";
            detailBody.setText(crops + "  •  " + qty + "  •  " + price +
                    "\nClick Marketplace to view and reserve the available produce.");
            detailCard.setVisible(true);
            detailCard.repaint();
        }

        private void showSchoolDetails() {
            detailEyebrow.setText("COMMUNITY LANDMARK");
            detailEyebrow.setForeground(BLUE_GRAY);
            detailTitle.setText("Waller High School");
            detailMeta.setText(SCHOOL_ADDRESS);
            detailBody.setText("HarvestHub uses the school as a familiar local reference point while you explore nearby farm and pickup listings.");
            detailCard.setVisible(true);
            detailCard.repaint();
        }

        private void showSearchDetails(String query) {
            detailEyebrow.setText("SEARCH RESULT");
            detailEyebrow.setForeground(ACCENT.darker());
            detailTitle.setText(query.length() > 42 ? query.substring(0, 40) + "…" : query);
            detailMeta.setText("Map search result");
            detailBody.setText("You can drag from here to explore the surrounding Waller area, or click Waller High School to return to the project home point.");
            detailCard.setVisible(true);
            detailCard.repaint();
        }

        private GeoPoint knownAreaPoint(String location) {
            String lower = location == null ? "" : location.toLowerCase();
            if (lower.contains("prairie view")) return new GeoPoint(30.0893, -95.9885);
            if (lower.contains("hempstead")) return new GeoPoint(30.0913, -96.0734);
            if (lower.contains("waller")) return new GeoPoint(30.058752, -95.926336);
            return null;
        }

        private boolean looksLikeStreetAddress(String location) {
            return location != null && location.matches(".*\\d.*");
        }

        private void geocode(String query, java.util.function.Consumer<GeoPoint> callback, boolean userInitiated) {
            String cacheKey = normalizeGeocodeKey(query);
            GeoPoint cached = pointFromCache(cacheKey);
            if (cached != null) {
                SwingUtilities.invokeLater(() -> callback.accept(cached));
                return;
            }

            geocodeExecutor.submit(() -> {
                GeoPoint result = null;
                try {
                    long wait = 1100L - (System.currentTimeMillis() - lastGeocodeRequestAt);
                    if (wait > 0) Thread.sleep(wait);

                    String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
                    URL url = new URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encoded);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(7000);
                    connection.setRequestProperty("User-Agent", MAP_USER_AGENT);
                    connection.setRequestProperty("Accept", "application/json");
                    lastGeocodeRequestAt = System.currentTimeMillis();

                    if (connection.getResponseCode() == 200) {
                        StringBuilder json = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) json.append(line);
                        }
                        result = parseFirstGeoPoint(json.toString());
                        if (result != null) {
                            saveGeocodePoint(cacheKey, result);
                        }
                    }
                    connection.disconnect();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                }

                GeoPoint finalResult = result;
                SwingUtilities.invokeLater(() -> callback.accept(finalResult));
            });
        }

        private GeoPoint parseFirstGeoPoint(String json) {
            Pattern latPattern = Pattern.compile("\\\"lat\\\"\\s*:\\s*\\\"(-?\\d+(?:\\.\\d+)?)\\\"");
            Pattern lonPattern = Pattern.compile("\\\"lon\\\"\\s*:\\s*\\\"(-?\\d+(?:\\.\\d+)?)\\\"");
            Matcher latMatcher = latPattern.matcher(json);
            Matcher lonMatcher = lonPattern.matcher(json);
            if (!latMatcher.find() || !lonMatcher.find()) return null;
            try {
                return new GeoPoint(Double.parseDouble(latMatcher.group(1)), Double.parseDouble(lonMatcher.group(1)));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private String normalizeGeocodeKey(String query) {
            return query.trim().toLowerCase().replaceAll("\\s+", " ");
        }

        private void loadGeocodeCache() {
            if (!geocodeCacheFile.exists()) return;
            try (InputStream in = new FileInputStream(geocodeCacheFile)) {
                geocodeDiskCache.load(in);
            } catch (IOException ignored) {
            }
        }

        private GeoPoint pointFromCache(String key) {
            String value = geocodeDiskCache.getProperty(key);
            if (value == null) return null;
            String[] parts = value.split(",");
            if (parts.length != 2) return null;
            try {
                return new GeoPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private void saveGeocodePoint(String key, GeoPoint point) {
            geocodeDiskCache.setProperty(key, point.lat + "," + point.lon);
            try (OutputStream out = new FileOutputStream(geocodeCacheFile)) {
                geocodeDiskCache.store(out, "HarvestHub map geocoding cache");
            } catch (IOException ignored) {
            }
        }

        private Point geoToScreen(double lat, double lon) {
            double cx = lonToWorldX(centerLon, zoom);
            double cy = latToWorldY(centerLat, zoom);
            double wx = lonToWorldX(lon, zoom);
            double wy = latToWorldY(lat, zoom);
            return new Point(
                    (int) Math.round(wx - cx + getWidth() / 2.0),
                    (int) Math.round(wy - cy + getHeight() / 2.0)
            );
        }

        private GeoPoint screenToGeo(int x, int y) {
            double cx = lonToWorldX(centerLon, zoom);
            double cy = latToWorldY(centerLat, zoom);
            double wx = cx + (x - getWidth() / 2.0);
            double wy = cy + (y - getHeight() / 2.0);
            return new GeoPoint(worldYToLat(wy, zoom), worldXToLon(wx, zoom));
        }

        private double lonToWorldX(double lon, int z) {
            double worldSize = TILE_SIZE * Math.pow(2, z);
            return (lon + 180.0) / 360.0 * worldSize;
        }

        private double latToWorldY(double lat, int z) {
            double clamped = Math.max(-85.05112878, Math.min(85.05112878, lat));
            double sin = Math.sin(Math.toRadians(clamped));
            double worldSize = TILE_SIZE * Math.pow(2, z);
            return (0.5 - Math.log((1 + sin) / (1 - sin)) / (4 * Math.PI)) * worldSize;
        }

        private double worldXToLon(double x, int z) {
            double worldSize = TILE_SIZE * Math.pow(2, z);
            return x / worldSize * 360.0 - 180.0;
        }

        private double worldYToLat(double y, int z) {
            double worldSize = TILE_SIZE * Math.pow(2, z);
            double n = Math.PI - (2.0 * Math.PI * y / worldSize);
            return Math.toDegrees(Math.atan(Math.sinh(n)));
        }

        private void clampCenter() {
            centerLat = Math.max(-85.0, Math.min(85.0, centerLat));
            while (centerLon > 180) centerLon -= 360;
            while (centerLon < -180) centerLon += 360;
        }

        private class MapMarker {
            final String farmName;
            final String location;
            final String publicLocation;
            final ArrayList<String> crops = new ArrayList<>();
            double totalQuantity = 0;
            boolean urgent = false;
            boolean hasFree = false;
            boolean allFree = true;
            boolean approximate = false;
            GeoPoint point;

            MapMarker(String farmName, String location, String publicLocation) {
                this.farmName = farmName;
                this.location = location;
                this.publicLocation = publicLocation;
            }

            void addListing(ProduceListing listing) {
                if (!crops.contains(listing.crop)) crops.add(listing.crop);
                totalQuantity += listing.quantity;
                urgent = urgent || listing.urgent;
                hasFree = hasFree || listing.donation;
                allFree = allFree && listing.donation;
            }

            String cropSummary() {
                if (crops.isEmpty()) return "Produce available";
                if (crops.size() == 1) return crops.get(0);
                if (crops.size() == 2) return crops.get(0) + " + " + crops.get(1);
                return crops.get(0) + ", " + crops.get(1) + " + " + (crops.size() - 2) + " more";
            }
        }

        private class GeoPoint {
            final double lat;
            final double lon;

            GeoPoint(double lat, double lon) {
                this.lat = lat;
                this.lon = lon;
            }
        }
    }


    // ============================================================
    // VALIDATION + PILOT EVIDENCE
    // ============================================================

    private JPanel createValidationPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(26, 30, 28, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleBlock(
                "Validation",
                "Keep competition evidence separate from demo data: real interviews, pilot outcomes, and expert feedback only."
        ), BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 2));
        actions.setOpaque(false);
        RoundedButton interview = new RoundedButton("+ Farmer interview", SAGE, FOREST_DARK);
        RoundedButton pilot = new RoundedButton("+ Pilot result", FOREST, Color.WHITE);
        RoundedButton expert = new RoundedButton("+ Expert feedback", new Color(238, 242, 246), BLUE_GRAY);
        actions.add(interview);
        actions.add(pilot);
        actions.add(expert);
        header.add(actions, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        ViewportWidthPanel content = new ViewportWidthPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        RoundedPanel integrity = new RoundedPanel(18, new Color(247, 244, 235));
        integrity.setBorder(new EmptyBorder(13, 16, 13, 16));
        integrity.setLayout(new BorderLayout());
        integrity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        integrity.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea integrityText = textArea(
                "Evidence rule: only enter interviews, testimonials, pounds rescued, or outcomes that actually happened. HarvestHub intentionally does not invent validation numbers.",
                11, new Color(103, 91, 70)
        );
        integrityText.setRows(2);
        integrity.add(integrityText, BorderLayout.CENTER);
        content.add(integrity);
        content.add(Box.createVerticalStrut(16));

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        validationInterviewLabel = new JLabel("0");
        validationPilotLabel = new JLabel("0 lbs");
        validationExpertLabel = new JLabel("0");
        validationBenchmarkLabel = new JLabel("—");
        stats.add(validationMetricCard("Farmer interviews", validationInterviewLabel, "Real conversations logged"));
        stats.add(validationMetricCard("Pilot produce", validationPilotLabel, "Real pounds in pilot records"));
        stats.add(validationMetricCard("Expert feedback", validationExpertLabel, "Real reviews or testimonials"));
        stats.add(validationMetricCard("Prototype match benchmark", validationBenchmarkLabel, "Current algorithm test data"));
        content.add(stats);
        content.add(Box.createVerticalStrut(18));

        JLabel evidenceTitle = cardTitle("Evidence log");
        content.add(evidenceTitle);
        content.add(Box.createVerticalStrut(10));
        validationListPanel = new ViewportWidthPanel();
        validationListPanel.setOpaque(false);
        validationListPanel.setLayout(new BoxLayout(validationListPanel, BoxLayout.Y_AXIS));
        validationListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(validationListPanel);
        content.add(Box.createVerticalStrut(18));

        ShadowPanel benchmark = new ShadowPanel(22, SURFACE);
        benchmark.setBorder(new EmptyBorder(18, 20, 18, 20));
        benchmark.setLayout(new BoxLayout(benchmark, BoxLayout.Y_AXIS));
        benchmark.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        benchmark.setAlignmentX(Component.LEFT_ALIGNMENT);
        benchmark.add(smallLabel("TECHNICAL BENCHMARK", FOREST));
        benchmark.add(Box.createVerticalStrut(5));
        benchmark.add(cardTitle("Measure the algorithm without pretending it is field accuracy"));
        benchmark.add(Box.createVerticalStrut(7));
        JTextArea benchmarkText = textArea(
                "HarvestHub can report prototype metrics such as top-match score and route-distance reduction from the current recipient profiles. Those are algorithm demonstrations, not claims that the system has been scientifically validated.",
                FONT_BODY, MUTED
        );
        benchmarkText.setRows(3);
        benchmark.add(benchmarkText);
        content.add(benchmark);

        page.add(cleanScroll(content), BorderLayout.CENTER);

        interview.addActionListener(e -> showValidationDialog("INTERVIEW"));
        pilot.addActionListener(e -> showValidationDialog("PILOT"));
        expert.addActionListener(e -> showValidationDialog("EXPERT"));
        refreshValidationPage();
        return page;
    }

    private JPanel validationMetricCard(String label, JLabel valueLabel, String note) {
        ShadowPanel card = new ShadowPanel(18, SURFACE);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel labelComponent = smallLabel(label.toUpperCase(), MUTED);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(TEXT);
        JLabel noteComponent = new JLabel(note);
        noteComponent.setFont(new Font("SansSerif", Font.PLAIN, 10));
        noteComponent.setForeground(MUTED);
        card.add(labelComponent);
        card.add(Box.createVerticalStrut(5));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(noteComponent);
        return card;
    }

    private void showValidationDialog(String type) {
        JTextField person = styledTextField();
        JTextField role = styledTextField();
        JTextField quantity = styledTextField();
        JTextArea notes = new JTextArea(5, 34);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        notes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 9, 8, 9)));

        JPanel form = new JPanel();
        form.setBackground(SURFACE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(labeledField(type.equals("PILOT") ? "Pilot partner / participant" : "Person or organization", person));
        form.add(Box.createVerticalStrut(8));
        form.add(labeledField("Role / context", role));
        if (type.equals("PILOT")) {
            form.add(Box.createVerticalStrut(8));
            form.add(labeledField("Real pounds rescued in this pilot result", quantity));
        }
        form.add(Box.createVerticalStrut(8));
        JLabel notesLabel = new JLabel(type.equals("EXPERT") ? "Feedback or testimonial summary" :
                type.equals("INTERVIEW") ? "What did you learn?" : "What actually happened?");
        notesLabel.setFont(new Font("SansSerif", Font.BOLD, FONT_FIELD_LABEL));
        notesLabel.setForeground(TEXT);
        form.add(notesLabel);
        form.add(Box.createVerticalStrut(5));
        form.add(new JScrollPane(notes));

        int result = JOptionPane.showConfirmDialog(this, form,
                type.equals("INTERVIEW") ? "Log farmer interview" : type.equals("PILOT") ? "Log pilot result" : "Log expert feedback",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (person.getText().trim().isEmpty() || notes.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter who the evidence came from and what you learned.");
            return;
        }

        double pounds = 0;
        if (type.equals("PILOT")) {
            try {
                pounds = Double.parseDouble(quantity.getText().trim());
                if (pounds < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid number of real pilot pounds.");
                return;
            }
        }

        ValidationEntry entry = new ValidationEntry(
                data.nextValidationId++, type, person.getText().trim(), role.getText().trim(),
                LocalDate.now().toString(), notes.getText().trim(), pounds
        );
        data.validationEntries.add(0, entry);
        saveData();
        refreshValidationPage();
    }

    private void refreshValidationPage() {
        if (validationListPanel == null) return;
        int interviews = 0;
        int experts = 0;
        double pilotPounds = 0;
        for (ValidationEntry entry : data.validationEntries) {
            if ("INTERVIEW".equals(entry.type)) interviews++;
            if ("EXPERT".equals(entry.type)) experts++;
            if ("PILOT".equals(entry.type)) pilotPounds += entry.pounds;
        }
        validationInterviewLabel.setText(String.valueOf(interviews));
        validationExpertLabel.setText(String.valueOf(experts));
        validationPilotLabel.setText(formatNumber(pilotPounds) + " lbs");
        validationBenchmarkLabel.setText(prototypeMatchBenchmark());

        validationListPanel.removeAll();
        if (data.validationEntries.isEmpty()) {
            validationListPanel.add(emptyState(
                    "No real-world evidence logged yet",
                    "Start with a farmer interview, then record pilots and expert feedback as they actually happen."
            ));
        } else {
            for (ValidationEntry entry : data.validationEntries) {
                validationListPanel.add(createValidationEntryCard(entry));
                validationListPanel.add(Box.createVerticalStrut(9));
            }
        }
        validationListPanel.revalidate();
        validationListPanel.repaint();
    }

    private JPanel createValidationEntryCard(ValidationEntry entry) {
        ShadowPanel card = new ShadowPanel(18, SURFACE);
        card.setBorder(new EmptyBorder(13, 16, 13, 16));
        card.setLayout(new BorderLayout(16, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel type = pill(entry.type, entry.type.equals("PILOT") ? SAGE :
                        entry.type.equals("EXPERT") ? new Color(238, 242, 246) : SOFT_ORANGE,
                entry.type.equals("EXPERT") ? BLUE_GRAY : FOREST_DARK);
        type.setPreferredSize(new Dimension(92, 26));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(entry.person + (entry.role.isBlank() ? "" : " • " + entry.role));
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT);
        JLabel date = new JLabel(entry.date + (entry.pounds > 0 ? " • " + formatNumber(entry.pounds) + " lbs" : ""));
        date.setFont(new Font("SansSerif", Font.PLAIN, 10));
        date.setForeground(MUTED);
        JTextArea note = textArea(entry.notes, 11, TEXT);
        note.setRows(2);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        copy.add(title);
        copy.add(Box.createVerticalStrut(3));
        copy.add(date);
        copy.add(Box.createVerticalStrut(5));
        copy.add(note);

        card.add(type, BorderLayout.WEST);
        card.add(copy, BorderLayout.CENTER);
        return card;
    }

    private String prototypeMatchBenchmark() {
        if (data.recipients.isEmpty() || data.listings.isEmpty()) return "—";
        double sum = 0;
        int count = 0;
        for (ProduceListing listing : data.listings) {
            List<MatchResult> matches = calculateMatches(listing);
            if (!matches.isEmpty()) {
                sum += matches.get(0).score;
                count++;
            }
        }
        return count == 0 ? "—" : Math.round(sum / count) + "% top fit";
    }

    // ============================================================
    // IMPACT
    // ============================================================

    private JPanel createImpactPanel() {
        JPanel content = pageContent();
        content.add(sectionHeader("Community Impact", "Small local actions add up over time."));
        content.add(Box.createVerticalStrut(20));

        JPanel stats = new JPanel(new GridLayout(2, 2, 16, 16));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        impactPoundsLabel = impactBigCard("Produce rescued", formatNumber(data.poundsRescued) + " lbs", "Food reserved instead of being left unused");
        impactFamiliesLabel = impactBigCard("Families helped", String.valueOf(data.familiesHelped), "Community reservations completed");
        impactDonationsLabel = impactBigCard("Donations completed", String.valueOf(data.donationsCompleted), "Free produce shared locally");
        impactListingsLabel = impactBigCard("Active listings", String.valueOf(data.listings.size()), "Produce currently available");

        stats.add((Component) impactPoundsLabel.getClientProperty("panel"));
        stats.add((Component) impactFamiliesLabel.getClientProperty("panel"));
        stats.add((Component) impactDonationsLabel.getClientProperty("panel"));
        stats.add((Component) impactListingsLabel.getClientProperty("panel"));
        content.add(stats);
        content.add(Box.createVerticalStrut(22));

        ShadowPanel message = new ShadowPanel(22, SAGE_LIGHT);
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));
        message.setBorder(new EmptyBorder(22, 24, 22, 24));
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        message.add(smallLabel("THE GOAL", FOREST));
        message.add(Box.createVerticalStrut(7));
        JLabel title = new JLabel("Measure whether the idea is actually helping.");
        title.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        title.setForeground(TEXT);
        message.add(title);
        message.add(Box.createVerticalStrut(8));
        message.add(textArea("HarvestHub is not just about posting food. These numbers show whether surplus produce is actually reaching people and staying in the community.", 14, MUTED));
        content.add(message);
        content.add(Box.createVerticalStrut(16));

        ShadowPanel pipeline = new ShadowPanel(22, SURFACE);
        pipeline.setLayout(new BoxLayout(pipeline, BoxLayout.Y_AXIS));
        pipeline.setBorder(new EmptyBorder(20, 24, 20, 24));
        pipeline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        pipeline.setAlignmentX(Component.LEFT_ALIGNMENT);
        pipeline.add(smallLabel("INTELLIGENCE PIPELINE", FOREST));
        pipeline.add(Box.createVerticalStrut(7));
        JLabel pipelineTitle = new JLabel("From a harvest signal to a measurable community outcome");
        pipelineTitle.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        pipelineTitle.setForeground(TEXT);
        pipeline.add(pipelineTitle);
        pipeline.add(Box.createVerticalStrut(9));
        JTextArea pipelineText = textArea(
                "HarvestScan  →  Harvest Intelligence  →  Smart Match  →  Route Optimizer  →  Reservation / Pilot Evidence",
                13, FOREST_DARK
        );
        pipelineText.setFont(new Font("SansSerif", Font.BOLD, 13));
        pipelineText.setRows(2);
        pipeline.add(pipelineText);
        content.add(pipeline);

        return wrapScrollable(content);
    }

    private JLabel impactBigCard(String label, String value, String note) {
        ShadowPanel panel = new ShadowPanel(22, SURFACE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(22, 22, 18, 22));
        JLabel labelLabel = smallLabel(label.toUpperCase(), MUTED);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setForeground(FOREST_DARK);
        JLabel noteLabel = new JLabel(note);
        noteLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noteLabel.setForeground(MUTED);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(labelLabel);
        panel.add(Box.createVerticalStrut(7));
        panel.add(valueLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(noteLabel);
        valueLabel.putClientProperty("panel", panel);
        return valueLabel;
    }

    private void refreshImpactLabels() {
        if (impactPoundsLabel != null) impactPoundsLabel.setText(formatNumber(data.poundsRescued) + " lbs");
        if (impactFamiliesLabel != null) impactFamiliesLabel.setText(String.valueOf(data.familiesHelped));
        if (impactDonationsLabel != null) impactDonationsLabel.setText(String.valueOf(data.donationsCompleted));
        if (impactListingsLabel != null) impactListingsLabel.setText(String.valueOf(data.listings.size()));
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private JPanel pageContent() {
        ViewportWidthPanel p = new ViewportWidthPanel();
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(26, 30, 30, 30));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel p = titleBlock(title, subtitle);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        return p;
    }

    private JPanel titleBlock(String titleText, String subtitleText) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, FONT_PAGE_TITLE));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, FONT_SUBTITLE));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(subtitle);
        return p;
    }

    private JLabel cardTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("SansSerif", Font.BOLD, FONT_CARD_TITLE));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }

    private JTextField styledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(FOREST);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(9, 11, 9, 11)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
        field.setPreferredSize(new Dimension(120, 41));
        return field;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(FOREST);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(9, 11, 9, 11)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
        return field;
    }

    private JComboBox<String> styledCombo(String[] values) {
        JComboBox<String> box = new JComboBox<>(values);
        box.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        box.setForeground(TEXT);
        box.setBackground(SURFACE);
        box.setBorder(BorderFactory.createLineBorder(BORDER));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
        box.setPreferredSize(new Dimension(140, 41));
        return box;
    }

    private JPanel labeledField(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, FONT_FIELD_LABEL));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(l);
        p.add(Box.createVerticalStrut(5));
        p.add(field);
        return p;
    }

    private JPanel fixedLabeledField(String label, JComponent field, int width) {
        JPanel p = labeledField(label, field);
        p.setPreferredSize(new Dimension(width, 66));
        p.setMinimumSize(new Dimension(width, 66));
        p.setMaximumSize(new Dimension(width, 66));
        field.setPreferredSize(new Dimension(width, 41));
        field.setMinimumSize(new Dimension(width, 41));
        field.setMaximumSize(new Dimension(width, 41));
        return p;
    }

    private JLabel smallLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, FONT_EYEBROW));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel pill(String text, Color background, Color foreground) {
        JLabel label = new JLabel(" " + text + " ");
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(new Font("SansSerif", Font.BOLD, FONT_EYEBROW));
        label.setBorder(new EmptyBorder(4, 7, 4, 7));
        return label;
    }

    private JTextArea textArea(String text, int size, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font("SansSerif", Font.PLAIN, size));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setMargin(new Insets(0, 0, 0, 0));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private JPanel emptyState(String titleText, String messageText) {
        ShadowPanel panel = new ShadowPanel(22, SURFACE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(32, 28, 32, 28));
        panel.setPreferredSize(new Dimension(380, 150));

        JLabel title = cardTitle(titleText);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = new JLabel(messageText);
        message.setFont(new Font("SansSerif", Font.PLAIN, FONT_BODY));
        message.setForeground(MUTED);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(title);
        panel.add(Box.createVerticalStrut(7));
        panel.add(message);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JScrollPane cleanScroll(Component content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setBackground(BG);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        return scroll;
    }

    private JPanel wrapScrollable(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(cleanScroll(content), BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshAllDynamicPanels() {
        refreshMarketplaceCards();
        refreshFavorites();
        refreshMyListings();
        refreshImpactLabels();
        refreshSmartMatchListings();
        refreshValidationPage();
        if (communityMapPanel != null) communityMapPanel.reloadFarmMarkers();
        if (pagePanel != null) pagePanel.repaint();
    }

    private String formatNumber(double number) {
        if (Math.abs(number - Math.rint(number)) < 0.00001) return String.valueOf((long) Math.rint(number));
        return String.format("%.1f", number);
    }

    // ============================================================
    // CUSTOM COMPONENTS
    // ============================================================

    /**
     * A panel that always follows the width of its JScrollPane viewport.
     * This keeps pages responsive and prevents horizontal scroll bars.
     */
    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        ViewportWidthPanel() {
            super();
        }

        ViewportWidthPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(60, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ShadowPanel extends JPanel {
        private final int radius;
        private final Color fill;

        ShadowPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
            setBorder(new EmptyBorder(5, 5, 7, 5));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(23, 49, 35, 13));
            g2.fillRoundRect(4, 5, getWidth() - 8, getHeight() - 9, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(1, 1, getWidth() - 7, getHeight() - 9, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        private final Color fill;
        private final Color textColor;
        private boolean hover = false;
        private boolean compact = false;

        RoundedButton(String text, Color fill, Color textColor) {
            super(text);
            this.fill = fill;
            this.textColor = textColor;
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setForeground(textColor);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 16, 10, 16));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setCompact(boolean compact) {
            this.compact = compact;
            setBorder(compact ? new EmptyBorder(8, 12, 8, 12) : new EmptyBorder(10, 16, 10, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color draw = getModel().isPressed() ? fill.darker() : (hover ? blend(fill, Color.WHITE, 0.08f) : fill);
            g2.setColor(draw);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), compact ? 15 : 18, compact ? 15 : 18);
            g2.dispose();
            setForeground(textColor);
            super.paintComponent(g);
        }

        private static Color blend(Color a, Color b, float t) {
            float u = 1f - t;
            return new Color((int)(a.getRed() * u + b.getRed() * t),
                    (int)(a.getGreen() * u + b.getGreen() * t),
                    (int)(a.getBlue() * u + b.getBlue() * t), a.getAlpha());
        }
    }

    private static class NavButton extends JButton {
        private boolean selected;
        private boolean hover;

        NavButton(String text) {
            super(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setForeground(new Color(218, 231, 222));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 13, 11, 13));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setNavSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) {
                g2.setColor(new Color(255, 255, 255, 32));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(186, 224, 198));
                g2.fillRoundRect(0, 8, 4, getHeight() - 16, 4, 4);
                setForeground(Color.WHITE);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                setForeground(Color.WHITE);
            } else {
                setForeground(new Color(205, 222, 211));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class LogoMark extends JPanel {
        LogoMark() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(230, 241, 233));
            g2.fillRoundRect(0, 0, w, h, 14, 14);
            g2.setColor(FOREST);
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w / 2, h - 9, w / 2, 15);
            g2.fillOval(w / 2 - 15, 12, 16, 10);
            g2.fillOval(w / 2, 20, 16, 10);
            g2.dispose();
        }
    }

    private static class ProduceIllustrationPanel extends JPanel {
        private final String crop;

        ProduceIllustrationPanel(String crop) {
            this.crop = crop == null ? "Produce" : crop;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(239, 245, 237));
            g2.fillRoundRect(0, 0, w, h, 24, 24);

            String c = crop.toLowerCase();
            if (c.contains("tomato")) drawTomato(g2, w, h);
            else if (c.contains("watermelon") || c.contains("melon")) drawWatermelon(g2, w, h);
            else if (c.contains("cucumber")) drawCucumber(g2, w, h);
            else if (c.contains("peach")) drawPeach(g2, w, h);
            else drawBasket(g2, w, h);
            g2.dispose();
        }

        private void drawTomato(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(215, 78, 68));
            g2.fillOval(w/2 - 31, h/2 - 22, 62, 56);
            g2.setColor(new Color(66, 120, 67));
            Polygon leaf = new Polygon();
            leaf.addPoint(w/2, h/2 - 28);
            leaf.addPoint(w/2 - 14, h/2 - 10);
            leaf.addPoint(w/2, h/2 - 15);
            leaf.addPoint(w/2 + 14, h/2 - 10);
            g2.fillPolygon(leaf);
        }

        private void drawWatermelon(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(81, 145, 73));
            g2.fillOval(w/2 - 38, h/2 - 25, 76, 55);
            g2.setColor(new Color(166, 207, 118));
            g2.setStroke(new BasicStroke(4));
            g2.drawArc(w/2 - 26, h/2 - 20, 52, 45, 70, 220);
        }

        private void drawCucumber(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(62, 125, 75));
            g2.rotate(-0.35, w/2.0, h/2.0);
            g2.fillRoundRect(w/2 - 42, h/2 - 15, 84, 30, 28, 28);
            g2.rotate(0.35, w/2.0, h/2.0);
        }

        private void drawPeach(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(238, 154, 99));
            g2.fillOval(w/2 - 30, h/2 - 26, 60, 58);
            g2.setColor(new Color(70, 130, 76));
            g2.fillOval(w/2 + 3, h/2 - 33, 24, 12);
        }

        private void drawBasket(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(199, 148, 84));
            g2.fillRoundRect(w/2 - 37, h/2 - 5, 74, 43, 10, 10);
            g2.setStroke(new BasicStroke(5));
            g2.drawArc(w/2 - 28, h/2 - 30, 56, 50, 0, 180);
            g2.setColor(new Color(82, 143, 77));
            g2.fillOval(w/2 - 20, h/2 - 20, 22, 22);
            g2.setColor(new Color(219, 87, 71));
            g2.fillOval(w/2 + 1, h/2 - 17, 22, 22);
        }
    }

    // ============================================================
    // SAVED DATA
    // ============================================================

    private AppData loadData() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = in.readObject();
                if (obj instanceof AppData) return (AppData) obj;
            } catch (IOException | ClassNotFoundException ignored) {
                // If a save file is damaged or from an incompatible version, start with fresh demo data.
            }
        }
        return createStarterData();
    }


    private void ensureDataCompatibility() {
        if (data == null) data = new AppData();
        if (data.users == null) data.users = new ArrayList<>();
        if (data.listings == null) data.listings = new ArrayList<>();
        if (data.recipients == null) data.recipients = new ArrayList<>();
        if (data.validationEntries == null) data.validationEntries = new ArrayList<>();
        if (data.nextRecipientId <= 0) data.nextRecipientId = 1;
        if (data.nextValidationId <= 0) data.nextValidationId = 1;
        for (User user : data.users) {
            if (user.favoriteListingIds == null) user.favoriteListingIds = new HashSet<>();
            if (user.city == null || user.city.isBlank()) user.city = "Waller";
            if (user.zipCode == null || user.zipCode.isBlank()) user.zipCode = "77484";
            if (user.streetAddress == null) user.streetAddress = "";
            if (user.latitude == 0 || user.longitude == 0) {
                Coordinate c = approximateCoordinateForLocation(user.city + ", TX " + user.zipCode);
                user.latitude = c.lat;
                user.longitude = c.lon;
                user.locationIsPrecise = false;
            }
        }
        if (data.recipients.isEmpty()) addPrototypeRecipients(data);
    }

    private void addPrototypeRecipients(AppData target) {
        target.recipients.add(new RecipientProfile(
                target.nextRecipientId++, "Waller Community Pantry (Demo)", "Food pantry",
                "Waller, TX", 30.0608, -95.9195, 120, true, true, "all", true));
        target.recipients.add(new RecipientProfile(
                target.nextRecipientId++, "Prairie View Family Pickup (Demo)", "Family pickup",
                "Prairie View, TX", 30.0930, -95.9870, 28, true, true,
                "berries, tomatoes, leafy, cucumbers, peaches", true));
        target.recipients.add(new RecipientProfile(
                target.nextRecipientId++, "Hempstead Community Kitchen (Demo)", "Community organization",
                "Hempstead, TX", 30.0974, -96.0783, 85, false, true, "all", true));
        target.recipients.add(new RecipientProfile(
                target.nextRecipientId++, "Waller Neighborhood Pickup (Demo)", "Community organization",
                "Waller, TX", 30.0478, -95.9420, 45, true, false,
                "tomatoes, melons, apples, root, cucumbers", true));
    }

    private void saveData() {
        if (data == null) return;
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(data);
        } catch (IOException e) {
            System.out.println("Could not save HarvestHub data: " + e.getMessage());
        }
    }

    private AppData createStarterData() {
        AppData starter = new AppData();
        User farmer = new User(starter.nextUserId++, "Jordan", "farmer", "farm123", "FARMER", "Johnson Family Farm");
        farmer.city = "Waller";
        farmer.zipCode = "77484";
        farmer.latitude = 30.0588;
        farmer.longitude = -95.9263;
        farmer.defaultPickupLocation = true;

        User neighbor = new User(starter.nextUserId++, "Maya", "neighbor", "hello123", "COMMUNITY", "");
        neighbor.city = "Waller";
        neighbor.zipCode = "77484";
        neighbor.latitude = 30.0588;
        neighbor.longitude = -95.9263;
        starter.users.add(farmer);
        starter.users.add(neighbor);

        starter.listings.add(new ProduceListing(
                starter.nextListingId++, farmer.id, "Tomatoes", "Johnson Family Farm", 25, 1.25,
                "Waller, TX", LocalDate.now().plusDays(3).toString(), false, false,
                "Picked this morning. Great for salsa or sandwiches."
        ));
        starter.listings.add(new ProduceListing(
                starter.nextListingId++, 0, "Peaches", "Sunny Acres Farm", 40, 0,
                "Prairie View, TX", LocalDate.now().plusDays(2).toString(), true, true,
                "Extra ripe peaches need a home soon."
        ));
        starter.listings.add(new ProduceListing(
                starter.nextListingId++, 0, "Watermelon", "Green Valley Farm", 60, 0.75,
                "Hempstead, TX", LocalDate.now().plusDays(5).toString(), false, false,
                "Sweet summer watermelons from this week's harvest."
        ));
        starter.listings.add(new ProduceListing(
                starter.nextListingId++, 0, "Cucumbers", "Waller Community Garden", 18, 0,
                "Waller, TX", LocalDate.now().plusDays(1).toString(), true, false,
                "Free community pickup while supplies last."
        ));
        addPrototypeRecipients(starter);
        return starter;
    }

    private static class AppData implements Serializable {
        private static final long serialVersionUID = 1L;
        ArrayList<User> users = new ArrayList<>();
        ArrayList<ProduceListing> listings = new ArrayList<>();
        ArrayList<RecipientProfile> recipients = new ArrayList<>();
        ArrayList<ValidationEntry> validationEntries = new ArrayList<>();
        int nextUserId = 1;
        int nextListingId = 1;
        int nextRecipientId = 1;
        int nextValidationId = 1;
        double poundsRescued = 0;
        int familiesHelped = 0;
        int donationsCompleted = 0;
    }

    private static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name;
        String username;
        String password;
        String role;
        String farmName;
        // Location is stored for matching, but precise addresses are never shown publicly.
        String streetAddress = "";
        String city = "Waller";
        String zipCode = "77484";
        boolean usePreciseLocation = false;
        boolean defaultPickupLocation = false;
        boolean locationIsPrecise = false;
        double latitude = 0;
        double longitude = 0;
        HashSet<Integer> favoriteListingIds = new HashSet<>();

        User(int id, String name, String username, String password, String role, String farmName) {
            this.id = id;
            this.name = name;
            this.username = username;
            this.password = password;
            this.role = role;
            this.farmName = farmName;
        }

        boolean isFarmer() { return "FARMER".equals(role); }
    }


    private static class RecipientProfile implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String name;
        String type;
        String location;
        double lat;
        double lon;
        double capacityLbs;
        boolean pickupToday;
        boolean acceptsDonations;
        String cropPreferences;
        boolean demo;

        RecipientProfile(int id, String name, String type, String location, double lat, double lon,
                         double capacityLbs, boolean pickupToday, boolean acceptsDonations,
                         String cropPreferences, boolean demo) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.location = location;
            this.lat = lat;
            this.lon = lon;
            this.capacityLbs = capacityLbs;
            this.pickupToday = pickupToday;
            this.acceptsDonations = acceptsDonations;
            this.cropPreferences = cropPreferences;
            this.demo = demo;
        }
    }

    private static class ValidationEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String type;
        String person;
        String role;
        String date;
        String notes;
        double pounds;

        ValidationEntry(int id, String type, String person, String role, String date, String notes, double pounds) {
            this.id = id;
            this.type = type;
            this.person = person;
            this.role = role == null ? "" : role;
            this.date = date;
            this.notes = notes;
            this.pounds = pounds;
        }
    }

    private static class ProduceListing implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        int ownerUserId;
        String crop;
        String farmName;
        double quantity;
        double price;
        String location;
        String deadline;
        boolean donation;
        boolean urgent;
        String note;

        ProduceListing(int id, int ownerUserId, String crop, String farmName, double quantity,
                       double price, String location, String deadline, boolean donation,
                       boolean urgent, String note) {
            this.id = id;
            this.ownerUserId = ownerUserId;
            this.crop = crop;
            this.farmName = farmName;
            this.quantity = quantity;
            this.price = price;
            this.location = location;
            this.deadline = deadline;
            this.donation = donation;
            this.urgent = urgent;
            this.note = note;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}

