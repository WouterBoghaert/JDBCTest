package be.vdab;

import java.util.InputMismatchException;
import java.util.Scanner;

public enum Menu {
    
    // membervariabelen
    
    INSTANCE;
    
    // andere methods
    
    public void toonMenu(){
        System.out.println("Maak je keuze uit onderstaande opties, tijp het cijfer van je keuze");
        System.out.println("1. Nieuwe rekening");
        System.out.println("2. Saldo consulteren");
        System.out.println("3. Overschrijven");
        try(Scanner scanner = new Scanner(System.in)){
            int keuze = scanner.nextInt();
            if ((keuze < 1) || (keuze > 3)){
                throw new InputMismatchException();
            }
            else{
                switch(keuze){
                    case 1:
                        try {
                            //RekeningManager rekeningManager = new RekeningManager();
                            //rekeningManager.nieuweRekening(scanner);
                            RekeningManager.INSTANCE.nieuweRekening(scanner);
                        }
                        catch(RekeningException ex){
                            System.out.println(ex.getMessage());
                        }
                        break;
                    case 2:
                        try {
//                            RekeningManager rekeningManager = new RekeningManager();
//                            rekeningManager.saldoConsulteren(scanner);
                            RekeningManager.INSTANCE.saldoConsulteren(scanner);
                        }
                        catch(RekeningException ex){
                            System.out.println(ex.getMessage());
                        }
                        break;
                    case 3:
                        try {
//                            RekeningManager rekeningManager = new RekeningManager();
//                            rekeningManager.overschrijven(scanner);
                            RekeningManager.INSTANCE.overschrijven(scanner);
                        }
                        catch(RekeningException ex){
                            System.out.println(ex.getMessage());
                        }
                        break;
                }
            }
        }
        catch(InputMismatchException ex){
            System.out.println("Geef een cijfer van 1 tot 3 in!");
        }
    }
}
