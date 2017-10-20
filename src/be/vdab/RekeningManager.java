package be.vdab;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

public enum RekeningManager {
    // membervariabelen
    INSTANCE;
    
    private static final String URL = "jdbc:mysql://localhost/bank?useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "vdab";
    // user en pw nog aanpassen? Cursist gebruiken?
    private static final String SELECT_REKENINGNR = 
        "select RekeningNr from rekeningen where RekeningNr = ?";
    private static final String INSERT_REKENINGNR =
        "insert into rekeningen (RekeningNr) values(?)";
    private static final String SELECT_REKENING_SALDO =
        "select RekeningNr, Saldo from rekeningen where RekeningNr = ?";
    private static final String UPDATE_SALDO_OVERSCHRIJVING =
        "update rekeningen set Saldo = ? where RekeningNr = ?";
    
    // andere methods
    
    // gebruiker tikt 1 in => nieuwe rekening aanmaken
    public void nieuweRekening(Scanner scanner) throws RekeningException{
        System.out.println("Geef het rekeningnummer op van de nieuwe rekening"); 
        //checken of dit werkt, of nieuwe scanner nodig is
        long rekeningnummer = scanner.nextLong(); 
        if (valideerRekeningNr(rekeningnummer)){
            voegRecordToe(rekeningnummer);
        }
    }
    
    private boolean valideerRekeningNr(long rekeningnr) throws RekeningException {
        //controle op 12 cijfers
        if (((int)(rekeningnr / 100_000_000_000L) >=1) && ((int)(rekeningnr / 100_000_000_000L) <=9)){
            // correct rekeningnr
            long eersteTienCijfers = (long)(rekeningnr / 100);
            int laatsteTweeCijfers = (int)(rekeningnr % 100);
            if(eersteTienCijfers % 97 == laatsteTweeCijfers){
                return true;
            }
            else {
                throw new RekeningException("Dit is geen correct rekeningnummer!");
            }
        }
        else {
            throw new RekeningException("Rekeningnummer moet 12 cijfers lang zijn!");
        }
    }
    
    private void voegRecordToe(long rekeningnummer) throws RekeningException{
        try(Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement statementSelect = connection.prepareStatement(SELECT_REKENINGNR);
            PreparedStatement statementInsert = connection.prepareStatement(INSERT_REKENINGNR)){
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            statementSelect.setLong(1, rekeningnummer);
            try(ResultSet resultSet = statementSelect.executeQuery()){
                if(resultSet.next()){
                    throw new RekeningException("Deze rekening bestaat al in de database!");
                }
                else {
                    statementInsert.setLong(1, rekeningnummer);
                    int aantalUpdates = statementInsert.executeUpdate();
                    System.out.println(aantalUpdates==1?"Rekeningnummer is succesvol toegevoegd aan de database":"Er is iets misgelopen met het toevoegen van het rekeningnummer aan de database");
                }
            }
            connection.commit();
        }
        catch(SQLException ex){
            ex.printStackTrace();
        }
    }
    
    // gebruiker tikt 2 in, saldo consulteren
    public void saldoConsulteren(Scanner scanner) throws RekeningException {
        System.out.println("Geef het rekeningnummer op van de te consulteren rekening");
        long rekeningnummer = scanner.nextLong();
        if (valideerRekeningNr(rekeningnummer)){
            // geeft exception indien foutief rekeningnummer
            try(Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)){
                //PreparedStatement statementSelect = connection.prepareStatement(SELECT_REKENING_SALDO)){
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);
//                statementSelect.setLong(1, rekeningnummer);
//                try(ResultSet resultSet = statementSelect.executeQuery()){
                    //if(resultSet.next()){
                if(getSaldoRekening(connection, rekeningnummer)!=null){
                    // rekeningnummer is gevonden in de database
//                        System.out.println("Het saldo van rekening " + resultSet.getLong("RekeningNr") +
//                            " is " + resultSet.getBigDecimal("Saldo"));
                    System.out.println("Het saldo van rekening " + rekeningnummer + " is " +
                            getSaldoRekening(connection, rekeningnummer));
                }
                else {
                    throw new RekeningException("Deze rekening is niet gevonden in de database!");
                }
                connection.commit();
                //}                
            }
            catch(SQLException ex){
                ex.printStackTrace();
            }
        }
    }
    
    // gebruiker titk 3 in, overschrijven
    public void overschrijven(Scanner scanner) throws RekeningException {
        System.out.println("Geef het rekeningnummer op van waaruit je de overschrijving doet.");
        long vanRekeningnummer = scanner.nextLong();
        if (valideerRekeningNr(vanRekeningnummer)){
            System.out.println("Geef het rekeningnummer op waarnaar toe je de overschrijving doet.");
            long naarRekeningnummer = scanner.nextLong();
            if(valideerRekeningNr(naarRekeningnummer)){
                if (vanRekeningnummer != naarRekeningnummer){
                    System.out.println("Geef het bedrag op dat overgeschreven moet worden.");
                    BigDecimal bedrag = scanner.nextBigDecimal();
                    if(bedrag.compareTo(BigDecimal.ZERO) > 0){
                        try(Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)){
                            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                            connection.setAutoCommit(false);
                            //if(bestaatRekening(connection, vanRekeningnummer) && bestaatRekening(connection, naarRekeningnummer)){
                            if((getSaldoRekening(connection, vanRekeningnummer) != null)&&(getSaldoRekening(connection, naarRekeningnummer) != null)){
                                BigDecimal saldoVan = getSaldoRekening(connection, vanRekeningnummer);
                                BigDecimal saldoNaar = getSaldoRekening(connection, naarRekeningnummer);
                                //System.out.println(saldoVan + " " + saldoNaar);
                                if(saldoVan.compareTo(bedrag) >= 0){
                                    // het saldo op de rekening is groter dan het over te schrijven bedrag
                                    PreparedStatement statementUpdateSaldo = connection.prepareStatement(UPDATE_SALDO_OVERSCHRIJVING);
                                    statementUpdateSaldo.setBigDecimal(1, saldoVan.subtract(bedrag));
                                    statementUpdateSaldo.setLong(2, vanRekeningnummer);
                                    statementUpdateSaldo.addBatch();
                                    statementUpdateSaldo.setBigDecimal(1, saldoNaar.add(bedrag));
                                    statementUpdateSaldo.setLong(2, naarRekeningnummer);                                    
                                    statementUpdateSaldo.addBatch();
                                    int [] aantalUpdates = statementUpdateSaldo.executeBatch();
                                    //System.out.println(aantalUpdates[0] + " " + aantalUpdates[1]);
                                    System.out.println(Arrays.stream(aantalUpdates).sum() == 2?"Overschrijving is opgeslaan in de database.":
                                            "Er is iets misgelopen met het opslaan van de overschrijving in de database.");
                                }
                                else {
                                    throw new RekeningException("Het saldo op de rekening van waaruit overgeschreven moet worden"
                                            + " is kleiner dan het bedrag dat overgeschreven moet worden!");
                                }
                            }
                            else {
                                throw new RekeningException("Een of beide rekening(en) bestaan niet in de database!");
                            }
                            connection.commit();
                        }
                        catch(SQLException ex){
                            ex.printStackTrace();
                        }
                    }
                    else {
                        throw new RekeningException("Het over te schrijven bedrag moet groter dan nul zijn!");
                    }       
                }
                else {
                    throw new RekeningException("De rekeningnummers moeten van elkaar verschillen");
                }                         
            }
        }
    }
    
//    private boolean bestaatRekening(Connection connection, long rekeningNummer) throws SQLException {
//        try(PreparedStatement statementSelect = connection.prepareStatement(SELECT_REKENINGNR)){
//            statementSelect.setLong(1, rekeningNummer);
//            try(ResultSet resultSet = statementSelect.executeQuery()){
//                return(resultSet.next());
//            }
//        }        
//    }
    
    private BigDecimal getSaldoRekening(Connection connection, long rekeningNummer) throws SQLException {
        try(PreparedStatement statementSelectSaldo = connection.prepareStatement(SELECT_REKENING_SALDO)){
            statementSelectSaldo.setLong(1, rekeningNummer);
            try(ResultSet resultSet = statementSelectSaldo.executeQuery()){
                if(resultSet.next()){
                    return resultSet.getBigDecimal("Saldo");
                }
                else return null;
            }
        }
    }   
}
