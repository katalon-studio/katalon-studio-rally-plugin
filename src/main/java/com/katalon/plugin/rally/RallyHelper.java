package com.katalon.plugin.rally;

public class RallyHelper {

    public static String convertToRallyStatus(String katalonStatus) {
        switch (katalonStatus) {
            case "PASSED":
                return "Pass";
            case "FAILED":
                return "Fail";
            case "ERROR":
                return "Error";
            default:
                return "Inconclusive";
        }
    }
}
