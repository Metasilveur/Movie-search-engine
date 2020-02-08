import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

// javac -classpath "C:\Users\Rober\OneDrive\Bureau\GLPO\sqlite-jdbc-3.27.2.1.jar;." Test.java
// java -classpath "C:\Users\Rober\OneDrive\Bureau\GLPO\sqlite-jdbc-3.27.2.1.jar;." Test
// jdbc:sqlite:C:\\Users\\Rober\\OneDrive\\Bureau\\GLPO\\bdfilm.sqlite

/*
SELECT id_film, titre
FROM rechercher_film
where titre match "trésor"
*/
/*

with filtre as (SELECT id_film
                FROM recherche_titre
                WHERE titre match "Blabla")

*/
public class Test{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

    	RechercheFilm recherche = new RechercheFilm("jdbc:sqlite:C:\\Users\\Rober\\OneDrive\\Bureau\\GLPO\\bdfilm.sqlite");

    	//System.out.println(recherche.retrouve(recherche.fromSimpleToSQL("AVANT 2005, PAYS fr")));

        String zut = recherche.retrouve("AVANT 1960");



        System.out.println(zut);         

        /*String wesh = "DE Pierre Jean OU AVANT 2017";
        System.out.println(wesh);
        String[] values = wesh.split(",");

        String currentString = null;

        ArrayList<String> test = new ArrayList<String>();
        ArrayList<String> testF = new ArrayList<String>();

        if(values[0].trim().matches("(TITRE|DE|AVEC|PAYS|EN|AVANT|APRES).*")){
            currentString = values[0];
            for(int i=1;i<values.length;i++){
                if(!values[i].trim().matches("(TITRE|DE|AVEC|PAYS|EN|AVANT|APRES).*"))
                {
                    currentString += ","
                    +values[i];
                }
                else{
                    test.add(currentString.trim());
                    currentString = values[i];
                }
                if(i==values.length-1)
                    test.add(currentString.trim());

            }

            if(values.length == 1)
                test.add(currentString.trim());   
        }
        else{

        }

        for (int i=0;i<test.size() ;i++ ) {
            System.out.println(test.get(i));
        }


        for(int i=0;i<test.size();i++)
        {
            String[] testOU = test.get(i).split("OU");

            if(testOU.length == 1){
                testF.add(testOU[0]);
            }
            else{
                String actualString = testOU[0];
                for(int j=1;j<testOU.length;j++)
                {
                    if(!testOU[j].trim().matches("(TITRE|DE|AVEC|PAYS|EN|AVANT|APRES).*"))
                    {
                        actualString += " OU "
                        +testOU[j];
                    }
                    else{
                        testF.add(actualString.trim());
                        actualString = "OU "+testOU[j];                        
                    }
                    if(j == testOU.length-1)
                        testF.add(actualString.trim());
                }
            }
        }

        for (int i=0;i<testF.size() ;i++ ) {
            System.out.println(testF.get(i));
        }*/


    	//String test =  recherche.retrouve(recherche.fromSimpleToSQL("AVANT 2005, PAYS fr"));

        //System.out.println(recherche.fromSimpleToSQL("APRES 2010, PAYS us"));
    	//System.out.println(recherche.fromSimpleToSQL("TITRE Avatar, DE James Cameron, AVEC Zoe Saldana, PAYS us, APRES 2010"));

        ArrayList<NomPersonne> realisateurs = new ArrayList<>();

        realisateurs.add(new NomPersonne("Michelle", "Obama"));
        realisateurs.add(new NomPersonne("Allain", "Souchon"));

        ArrayList<NomPersonne> acteurs = new ArrayList<>();

        acteurs.add(new NomPersonne("Jean", "Eustache"));
        acteurs.add(new NomPersonne("Poulet", "Ticka"));

        ArrayList<String> autres_titres = new ArrayList<>();

        autres_titres.add("La maitre de l'air");

    	InfoFilm info = new InfoFilm("Avatar", realisateurs, acteurs, "us", 2009, 180,autres_titres);

    	//System.out.println(info.toString());

    	recherche.fermeBase();

    }
}