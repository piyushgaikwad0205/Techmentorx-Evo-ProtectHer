package com.example.sos;

public class EmergencyConfig {
    // Testing Mode - Set to false for production
    public static final boolean TESTING_MODE = true;
    public static final String TEST_NUMBER = "+919226144288";

    // Emergency Authority Numbers (Production)
    public static final String POLICE_NUMBER = "100";
    public static final String AMBULANCE_NUMBER = "102";
    public static final String FIRE_NUMBER = "101";
    public static final String DISASTER_NUMBER = "108";
    public static final String GAS_LEAK_NUMBER = "1906";

    // Emergency Types
    public static final String TYPE_POLICE = "POLICE";
    public static final String TYPE_DOCTOR = "DOCTOR";
    public static final String TYPE_AMBULANCE = "AMBULANCE";
    public static final String TYPE_ACCIDENT = "ACCIDENT";
    public static final String TYPE_FIRE = "FIRE";
    public static final String TYPE_DISASTER = "DISASTER";
    public static final String TYPE_GAS_LEAK = "GAS_LEAK";
    public static final String TYPE_GENERAL = "GENERAL";

    // Editable SOS Types
    public static final String TYPE_SOS_MEDICAL = "SOS_MEDICAL";
    public static final String TYPE_SOS_CONTACTS = "SOS_CONTACTS";
    public static final String TYPE_SOS_KEYWORDS = "SOS_KEYWORDS";

    // Professional Fixed Messages (Default)
    public static final String MSG_POLICE = "🚨 POLICE EMERGENCY! CRITICAL ALERT!\nI am in immediate danger and require police assistance at my location. Please respond immediately.";
    public static final String MSG_AMBULANCE = "🚑 AMBULANCE EMERGENCY! MEDICAL CRITICAL!\nLife-threatening medical emergency. Patient assistance required immediately at my location.";
    public static final String MSG_FIRE = "🔥 FIRE EMERGENCY! CRITICAL ALERT!\nA life-threatening fire has been reported at my location. Immediate fire department intervention is required.";
    public static final String MSG_DISASTER = "🌪️ DISASTER ALERT! CRITICAL EMERGENCY!\nI am trapped in a disaster-affected area and require urgent rescue and medical extraction.";
    public static final String MSG_GAS_LEAK = "☢️ GAS LEAK ALERT! CRITICAL HAZARD!\nA dangerous gas leak has been detected at my location. Urgent intervention required to prevent explosion/poisoning.";

    public static String getEmergencyNumber(String type) {
        if (TESTING_MODE) {
            return TEST_NUMBER;
        }

        switch (type) {
            case TYPE_POLICE:
                return POLICE_NUMBER;
            case TYPE_AMBULANCE:
            case TYPE_ACCIDENT:
            case TYPE_DOCTOR:
                return AMBULANCE_NUMBER;
            case TYPE_FIRE:
                return FIRE_NUMBER;
            case TYPE_DISASTER:
                return DISASTER_NUMBER;
            case TYPE_GAS_LEAK:
                return GAS_LEAK_NUMBER;
            default:
                return TEST_NUMBER;
        }
    }

    public static String formatEmergencyMessage(String type, UserProfile profile, String location,
            android.content.Context context) {
        String prefix = TESTING_MODE ? "[TEST MODE]\n" : "";
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());

        // For Medical (Ambulance or SOS_MEDICAL)
        if (type.equals(TYPE_AMBULANCE) || type.equals(TYPE_SOS_MEDICAL)) {
            return formatMedicalAlert(prefix, profile, location);
        }

        StringBuilder sb = new StringBuilder(prefix);
        String header = "🆘 SOS ALERT";
        if (profile.spokenKeyword != null && !profile.spokenKeyword.isEmpty()) {
            header += " Sender Speak " + profile.spokenKeyword.toUpperCase() + " keyword";
        }
        sb.append(header).append("\n");
        sb.append("Name: ").append(profile.name).append("\n");

        // Determine message content
        String customMsg = "";
        switch (type) {
            case TYPE_POLICE:
                customMsg = MSG_POLICE;
                break;
            case TYPE_FIRE:
                customMsg = MSG_FIRE;
                break;
            case TYPE_DISASTER:
                customMsg = MSG_DISASTER;
                break;
            case TYPE_GAS_LEAK:
                customMsg = MSG_GAS_LEAK;
                break;
            default:
                android.content.SharedPreferences prefs = context.getSharedPreferences("SosConfig",
                        android.content.Context.MODE_PRIVATE);
                if (type.equals(TYPE_SOS_CONTACTS))
                    customMsg = prefs.getString("msg_contacts", "I need help! Please check my location.");
                else if (type.equals(TYPE_SOS_KEYWORDS))
                    customMsg = prefs.getString("msg_keywords", "Keyword triggered SOS! I might be in danger.");
                else
                    customMsg = "Immediate help required.";
                break;
        }

        sb.append("Message: ").append(customMsg).append("\n");
        sb.append("Location: ").append(location).append("\n");
        sb.append("Time: ").append(timestamp).append("\n");

        sb.append("Battery: ").append(profile.mobileBattery).append("%");
        if (profile.watchBattery > 0) {
            sb.append(" (Watch: ").append(profile.watchBattery).append("%)");
        }

        return sb.toString();
    }

    private static String formatMedicalAlert(String prefix, UserProfile profile, String location) {
        StringBuilder sb = new StringBuilder(prefix);
        if (profile.heartRate > 0) {
            sb.append("🚨 MEDICAL EMERGENCY - LOW HEART RATE\n");
            sb.append("Heart Rate: ").append(profile.heartRate).append(" BPM (CRITICAL)\n");
        } else {
            String medicalHeader = "🚑 MEDICAL SOS";
            if (profile.spokenKeyword != null && !profile.spokenKeyword.isEmpty()) {
                medicalHeader = "🎙️ VOICE SOS (Keyword: " + profile.spokenKeyword.toUpperCase() + ")";
            }
            sb.append(medicalHeader).append("\n");
        }

        sb.append("\n--- MEDICAL INFO ---\n");
        sb.append("Name: ").append(profile.name).append("\n");
        sb.append("Blood: ").append(profile.bloodType).append("\n");
        if (!profile.allergies.isEmpty())
            sb.append("Allergies: ").append(profile.allergies.replace("\n", " ")).append("\n");
        if (!profile.medicalConditions.isEmpty())
            sb.append("Conditions: ").append(profile.medicalConditions.replace("\n", " ")).append("\n");
        if (!profile.medicines.isEmpty())
            sb.append("Meds: ").append(profile.medicines.replace("\n", " ")).append("\n");

        sb.append("\n--- BATTERY STATUS ---\n");
        sb.append("Mobile: ").append(profile.mobileBattery).append("%\n");
        if (profile.watchBattery > 0) {
            sb.append("Watch: ").append(profile.watchBattery).append("%\n");
        }

        sb.append("\nLocation: ").append(location);
        sb.append("\n\nIMMEDIATE MEDICAL ATTENTION REQUIRED!");

        return sb.toString();
    }

    public static class UserProfile {
        public String name = "";
        public String age = "";
        public String gender = "";
        public String mobile = "";
        public String emergencyContact = "";
        public String city = "";
        public String bloodType = "";
        public String allergies = "";
        public String medicalConditions = "";
        public String medicines = "";
        public int heartRate = 0;
        public int mobileBattery = 0;
        public int watchBattery = 0;
        public String spokenKeyword = "";
    }
}
