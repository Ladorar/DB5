/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package db5;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

/**
 *
 * @author Nicholas
 */
public class DB5 {

    
    private static boolean hasCustomerInfoCached = false;
    private static boolean hasCorrectCustomerNumber = false;
    private static int cachedCustomerNumber = -1;

    private static final String insertIntoPassagier = "insert into istmawarn.passagier values(?, ?, ?)";
    private static final String selectFromPassagier = "select kundennummer from istmawarn.passagier;";
    private static final String selectFlightToParam = "select istmawarn.abflug.abflugdatum, istmawarn.flug.flugnummer, one.iata_code as ziel_kurzel, one.name as ziel_name, two.iata_code as beginn_kurzel, two.name beginn_name\n"
            + "from istmawarn.abflug natural join istmawarn.flug inner join istmawarn.flughafen as one on (one.iata_code = istmawarn.flug.iata_code) inner join istmawarn.flughafen as two on (one.iata_code != two.iata_code)\n"
            + "where two.iata_code = istmawarn.flug.flu_iata_code and (one.iata_code like ? or one.name like ?)\n"
            + "order by istmawarn.abflug.abflugdatum;";
    private static final String insertIntoBuchen = "insert into buchen values (?, ?, ?, 129);";

    private static PreparedStatement FromPassagierStmt;
    private static PreparedStatement insertIntoPassagierStmt;
    private static PreparedStatement getFlightsToStmt;
    private static PreparedStatement insertIntoBuchenStmt;

private static LinkedList<Integer> customerNumbers = new LinkedList<Integer>();


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            String url = "jdbc:postgresql://141.100.70.56/istjuann";
            Class.forName("org.postgresql.Driver");
            Connection con = DriverManager.getConnection(url, "annenreul", "123");
            System.out.println("Connected to " + url);

            FromPassagierStmt = con.prepareStatement(selectFromPassagier);
            insertIntoPassagierStmt = con.prepareStatement(insertIntoPassagier);
            getFlightsToStmt = con.prepareStatement(selectFlightToParam);
            insertIntoBuchenStmt = con.prepareStatement(insertIntoBuchen);

            int user_choice = 0;
            do {
                printMenu();
                user_choice = Integer.parseInt(getInput("Bitte wählen Sie eine Option: "));
                selectFunctionByUserInput(user_choice);
            } while (user_choice != 0);

        } catch (Exception ex) {
            Logger.getLogger(DB5.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void printMenu() {
        System.out.println("System für das Buchen von Flügen:\n"
                + "1. Kundennummer eingeben.\n"
                + "2. Flugziel eingeben.\n"
                + "3. Flug buchen.\n"
                + "0. Beenden.");
    }

    private static String getInput(String printedString) {
        Scanner scan = new Scanner(System.in);
        System.out.print(printedString);
        return scan.next();
    }

    private static void selectFunctionByUserInput(int user_choice) throws SQLException {
        switch (user_choice) {
            case 0:
                return;
            case 1:
                getCustomerNumber();
                break;
            case 2:
                getFlightDestination();
                break;
            case 3:
                bookFlight();
                break;
            default:
                System.out.println("Ungültige Eingabe!");
        }
    }

    private static void getCustomerNumber() throws SQLException {
        //Wenn wir die Kundennummern noch nicht geholt haben machen wir es jetzt
        if (!hasCustomerInfoCached) {
            ResultSet rs = FromPassagierStmt.executeQuery();
            while (rs.next()) {
                int number = rs.getInt("kundennummer");
                customerNumbers.add(number);
                System.out.println(number);
            }
            hasCustomerInfoCached = true;
        }
        //Den Kunden nach seiner Nummer Fragen
        int customerNumber = Integer.parseInt(getInput("Bitte geben Sie ihre Kundennummer ein: "));
        if (customerNumbers.contains(customerNumber)) {
            hasCorrectCustomerNumber = true;
            cachedCustomerNumber = customerNumber;
            System.out.println("Richtige Eingabe. Sie können nun Flüge betrachten und Buchen.");
        } else {
            System.out.println("Es wird für Sie ein neuer Eintrag erstellt!");
            String firstname = getInput("Bitte geben Sie Ihren Vornamen an: ");
            String lastname = getInput("Bitte geben Sie Ihren Nachnamen an: ");
            insertIntoPassagierStmt.setInt(1, customerNumber);
            insertIntoPassagierStmt.setString(2, firstname);
            insertIntoPassagierStmt.setString(3, lastname);
            int changedRows = insertIntoPassagierStmt.executeUpdate();
            if (changedRows > 0) {
                cachedCustomerNumber = customerNumber;
                hasCorrectCustomerNumber = true;
            }
        }
    }

    private static void getFlightDestination() throws SQLException {
        if (hasCorrectCustomerNumber) {
            String input = getInput("Bitte geben Sie Ihr Ziel ein: ");
            getFlightsToStmt.setString(1, "%" + input + "%");
            getFlightsToStmt.setString(2, "%" + input + "%");
            ResultSet rs = getFlightsToStmt.executeQuery();
            while (rs.next()) {
                System.out.println("Flugnummer: " + rs.getString("flugnummer") + "Abflugdatum: " + rs.getString("abflugdatum") + " Startflughafen: " + rs.getString("beginn_kurzel") + " - " + rs.getString("beginn_name") + " ---> Zielflughafen: " + rs.getString("ziel_kurzel") + " - " + rs.getString("ziel_name"));
            }
        } else {
            System.out.println("Sie müssen zuerst ihre Kundennummer eingeben.");
        }
    }

    private static void bookFlight() throws SQLException {
        if (hasCorrectCustomerNumber) {
            String input = getInput("Bitte geben Sie die Flugnummer an: ");
            insertIntoBuchenStmt.setString(1, input);
            input = getInput("Bitte geben Sie das Abflugdatum an (YYYY/MM/DD): ");
            String[] splitted_input = input.split("/");
            int[] integer_input = new int[3];
            for (int i = 0; i < splitted_input.length; i++) {
                integer_input[i] = Integer.parseInt(splitted_input[i]);
                System.out.println("var: " + integer_input[i]);
            }
            //TODO fix dis shit
            insertIntoBuchenStmt.setDate(2, Date.valueOf(LocalDate.of(integer_input[0], integer_input[1], integer_input[2])));
            insertIntoBuchenStmt.setInt(3, cachedCustomerNumber);
            insertIntoBuchenStmt.setInt(4, 129);
            int changed_rows = insertIntoBuchenStmt.executeUpdate();
            if (changed_rows > 0) {
                System.out.println("Ihr Flug wurde erfolgreich gebucht!");
            } else {
                System.out.println("Es ist ein Fehler beim Buchen aufgetreten, bitte versuchen Sie es erneut.");
            }
        } else {
            System.out.println("Sie müssen zuerst ihre Kundennummer eingeben.");
        }
}
    
}
