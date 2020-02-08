import java.sql.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
*
*@author : Robert Bui & Maelle Laurence Tolsy
*/

/**
 *   Moteur de recherche de films
 *   <p>
 *  Cette classe charge une base de donée et extrait en json une requete simplifié
 *   
 */
public class RechercheFilm{

    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    String request = "";
    String currentTitle = null;
    String currentOtherTitle = null;
    int currentAnnee;
    int currentDuree;
    String currentPays = null;
    PreparedStatement pstmt = null;
    ArrayList<NomPersonne> acteurs = null;
    ArrayList<NomPersonne> realisateurs = null;
    ArrayList<String> autres_titres = null;

    ArrayList<String> word;
    ArrayList<String> type;
	ArrayList<String> rq;

    /**
     *    Constructor
     *
     *    @param nomFichierSQLite Path vers la base de donnée
     *    
     */

	public RechercheFilm(String nomFichierSQLite){

		request = "SELECT f.id_film,f.titre,f.annee,f.pays,f.duree,group_concat(a.titre, '|') AS autre_titres,p.prenom,p.nom,g.role"
		+ " FROM filtre JOIN films f ON f.id_film = filtre.id_film JOIN pays py ON py.code = f.pays LEFT JOIN autres_titres a ON a.id_film = f.id_film JOIN generique g ON g.id_film = f.id_film JOIN personnes p ON p.id_personne = g.id_personne"
		+ " GROUP BY f.id_film,f.titre,f.annee,p.prenom,p.nom,g.role;";

        try {

            String url = nomFichierSQLite;

            conn = DriverManager.getConnection(url);
            
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
	}

	public void fermeBase(){

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }

	}

    /**
     *     Lis la requete, extrait la base de donnée et retourne un json
     *     @param requete Requete simplifié
     *     @return Retourne le json correspondant a la requete
     */

	public String retrouve(String requete){

		StringBuilder json = new StringBuilder();

		StringBuilder filtre = new StringBuilder();

		filtre.append("WITH filtre AS ( ");

		word = new ArrayList<String>();
		type = new ArrayList<String>();
		rq = new ArrayList<String>();

        String[] values = requete.split(",");

        String currentString = null;

        ArrayList<String> test = new ArrayList<String>();

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

        for(int i=0;i<test.size();i++)
        {
            String[] testOU = test.get(i).split("OU");

            if(testOU.length == 1){
                rq.add(testOU[0]);
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
                        rq.add(actualString.trim());
                        actualString = "OU "+testOU[j];                        
                    }
                    if(j == testOU.length-1)
                        rq.add(actualString.trim());
                }
            }
        }

        for (int i=0;i<rq.size() ;i++ ) {
            System.out.println(rq.get(i));
        }



		boolean beginOU = false;
		int cTipart = 0;

		for(int i=0;i<rq.size();i++){

			if(i!=rq.size()-1){

				if((rq.get(i+1).trim().startsWith("OU ")) && (cTipart == 0)){
					beginOU = true;
					cTipart = 1;
					filtre.append("SELECT id_film from ( ");
					rq.set(i+1,rq.get(i+1).substring(3,rq.get(i+1).length()));
				}
				else if((rq.get(i+1).trim().startsWith("OU ")) && (cTipart == 1)){
					beginOU = true;
					rq.set(i+1,rq.get(i+1).substring(3,rq.get(i+1).length()));
				}
				else{
					beginOU = false;
					if(cTipart == 1)
						cTipart = 2;
				}
			}

			if(rq.get(i).trim().startsWith("TITRE "))
			{

				String[] values2 = rq.get(i).substring(6,rq.get(i).length()).split("OU");

				if(values2.length>1){
					filtre.append("SELECT id_film from ( ");
					for(int j=0; j<values2.length; j++){
						if(j!=values2.length-1){
							filtre.append("select id_film from  films where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%' union select  id_film from  autres_titres where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%' UNION ");
							word.add(values2[j].trim());
							type.add("String");
							word.add(values2[j].trim());
							type.add("String");
						}
						else{
							filtre.append("select id_film from  films where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%' union select  id_film from  autres_titres where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%'");
							word.add(values2[j].trim());
							type.add("String");
							word.add(values2[j].trim());
							type.add("String");
						}
				}
				filtre.append(" ) ");
			}
			else{
							filtre.append("select id_film from  films where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%' union select  id_film from  autres_titres where titre like  '%' ||  replace( ? ,  ' ',  '%')  ||  '%'");
							word.add(values2[0].trim());
							type.add("String");
							word.add(values2[0].trim());
							type.add("String");				
			}

			}
			else if(rq.get(i).trim().startsWith("DE "))
			{
				values = rq.get(i).substring(3,rq.get(i).length()).trim().split(",");

				
				if(values.length>1){

					for(int k=0;k<values.length;k++)
					{
						System.out.println(values[k]);
						String[] values2 = values[k].split("OU");

						if(values2.length>1){



						}
						else{

							String[] combi = values2[0].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;

								}

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									if(k == values.length-1){
										filtre.append(" ) ");
									}
									else{
										filtre.append(" ) INTERSECT ");
									}
								}

						}
					}

				}

				else{


					String[] values2 = values[0].split("OU");

					if(values2.length>1){////

						filtre.append("SELECT id_film from ( ");

						for(int j=0; j<values2.length; j++){

							if(j!=values2.length-1){

								String[] combi = values2[j].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;

								}

								System.out.println(aList);

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) UNION ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" ) UNION ");
								}
							}//////
							else{
								String[] combi = values2[j].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;

								}

								System.out.println(aList);

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" ) ");
								}
							}
						}

						filtre.append(" ) ");

					}
					else{

						String[] combi = rq.get(0).substring(3,rq.get(0).length()).trim().split(" ");
						ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
						int begin = 0;
						int end = combi.length;

						for (int l=0;l<combi.length;l++ ){
							String leNom = "";
							String lePrenom = "";
							String leNomAsiat = "";
							String lePrenomAsiat = "";

							for(int m=0;m<combi.length;m++){
								if(m<begin){
									leNom += combi[m] +" ";
									lePrenomAsiat += combi[m] +" ";
								}
								else{
									lePrenom += combi[m] +" ";
									leNomAsiat += combi[m] +" ";
								}
							}

							ArrayList<String> listFinal = new ArrayList<>();
							ArrayList<String> listFinal2 = new ArrayList<>();

							listFinal.add(leNom);
							listFinal.add(lePrenom);

							listFinal2.add(leNomAsiat);
							listFinal2.add(lePrenomAsiat);

							aList.add(listFinal);
							if(l!=0)
								aList.add(listFinal2);

							begin++;
							end--;

						}

						System.out.println(aList);

						if(aList.size()>1){

							filtre.append("SELECT id_film from ( ");
							filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");

							for(int m=1;m<aList.size();m++){
								filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
								word.add(aList.get(m).get(0).trim());
								type.add("String");
								word.add(aList.get(m).get(1).trim());
								type.add("String");
							}

							filtre.append(" ) ");
						}
						else{
							filtre.append("SELECT id_film from ( ");
							filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'R') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" ) ");
						}

					}

				}

			}
			else if(rq.get(i).trim().startsWith("AVEC "))
			{
				values = rq.get(i).substring(5,rq.get(i).length()).trim().split(",");

				
				if(values.length>1){

					for(int k=0;k<values.length;k++)
					{
						System.out.println(values[k]);
						String[] values2 = values[k].split("OU");

						if(values2.length>1){

							

						}
						else{

							String[] combi = values2[0].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;

								}

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									if(k == values.length-1){
										filtre.append(" ) ");
									}
									else{
										filtre.append(" ) INTERSECT ");
									}
								}

						}
					}

				}

				else{


					String[] values2 = values[0].split("OU");

					if(values2.length>1){////

						filtre.append("SELECT id_film from ( ");

						for(int j=0; j<values2.length; j++){

							if(j!=values2.length-1){

								String[] combi = values2[j].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;

								}

								System.out.println(aList);

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) UNION ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" ) UNION ");
								}
							}//////
							else{
								String[] combi = values2[j].trim().split(" ");
								ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
								int begin = 0;
								int end = combi.length;

								for (int l=0;l<combi.length;l++ ){
									String leNom = "";
									String lePrenom = "";
									String leNomAsiat = "";
									String lePrenomAsiat = "";

									for(int m=0;m<combi.length;m++){
										if(m<begin){
											leNom += combi[m] +" ";
											lePrenomAsiat += combi[m] +" ";
										}
										else{
											lePrenom += combi[m] +" ";
											leNomAsiat += combi[m] +" ";
										}
									}

									ArrayList<String> listFinal = new ArrayList<>();
									ArrayList<String> listFinal2 = new ArrayList<>();

									listFinal.add(leNom);
									listFinal.add(lePrenom);

									listFinal2.add(leNomAsiat);
									listFinal2.add(lePrenomAsiat);

									aList.add(listFinal);
									if(l!=0)
										aList.add(listFinal2);

									begin++;
									end--;
								}

								System.out.println(aList);

								if(aList.size()>1){

									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");

									for(int m=1;m<aList.size();m++){
										filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
										word.add(aList.get(m).get(0).trim());
										type.add("String");
										word.add(aList.get(m).get(1).trim());
										type.add("String");
									}

									filtre.append(" ) ");
								}
								else{
									filtre.append("SELECT id_film from ( ");
									filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
									word.add(aList.get(0).get(1).trim());
									type.add("String");
									filtre.append(" ) ");
								}
							}
						}

						filtre.append(" ) ");

					}
					else{

						String[] combi = rq.get(0).substring(3,rq.get(0).length()).trim().split(" ");
						ArrayList<ArrayList<String> > aList = new ArrayList<ArrayList<String>>();
						int begin = 0;
						int end = combi.length;

						for (int l=0;l<combi.length;l++ ){
							String leNom = "";
							String lePrenom = "";
							String leNomAsiat = "";
							String lePrenomAsiat = "";

							for(int m=0;m<combi.length;m++){
								if(m<begin){
									leNom += combi[m] +" ";
									lePrenomAsiat += combi[m] +" ";
								}
								else{
									lePrenom += combi[m] +" ";
									leNomAsiat += combi[m] +" ";
								}
							}

							ArrayList<String> listFinal = new ArrayList<>();
							ArrayList<String> listFinal2 = new ArrayList<>();

							listFinal.add(leNom);
							listFinal.add(lePrenom);

							listFinal2.add(leNomAsiat);
							listFinal2.add(lePrenomAsiat);

							aList.add(listFinal);
							if(l!=0)
								aList.add(listFinal2);

							begin++;
							end--;

						}

						System.out.println(aList);

						if(aList.size()>1){

							filtre.append("SELECT id_film from ( ");
							filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");

							for(int m=1;m<aList.size();m++){
								filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ? AND nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
								word.add(aList.get(m).get(0).trim());
								type.add("String");
								word.add(aList.get(m).get(1).trim());
								type.add("String");
							}

							filtre.append(" ) ");
						}
						else{
							filtre.append("SELECT id_film from ( ");
							filtre.append("SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE nom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" UNION SELECT FILMS.id_film from FILMS INNER JOIN (SELECT GENERIQUE.id_film FROM GENERIQUE INNER JOIN (SELECT id_personne FROM PERSONNES WHERE prenom like ?) A on A.id_personne = GENERIQUE.id_personne WHERE GENERIQUE.role = 'A') B ON B.id_film = FILMS.id_film");
							word.add(aList.get(0).get(1).trim());
							type.add("String");
							filtre.append(" ) ");
						}

					}

				}
			}
			else if(rq.get(i).trim().startsWith("PAYS "))
			{
				String[] values2 = rq.get(i).substring(5,rq.get(i).length()).split("OU");

				if(values2.length>1){
					filtre.append("SELECT id_film from ( ");
					for(int j=0; j<values2.length; j++){
						if(j!=values2.length-1){
							filtre.append("select id_film FROM (SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE nom like ?) A ON FILMS.pays like A.code union SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE code like ?) A ON FILMS.pays like A.code) UNION ");
							word.add(values2[j].trim());
							type.add("String");
							word.add(values2[j].trim());
							type.add("String");
						}
						else{
							filtre.append("select id_film FROM (SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE nom like ?) A ON FILMS.pays like A.code union SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE code like ?) A ON FILMS.pays like A.code)");
							word.add(values2[j].trim());
							type.add("String");
							word.add(values2[j].trim());
							type.add("String");
						}
				}
				filtre.append(" ) ");
			}
			else{
							filtre.append("select id_film FROM (SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE nom like ?) A ON FILMS.pays like A.code union SELECT id_film from FILMS INNER JOIN (SELECT code FROM PAYS WHERE code like ?) A ON FILMS.pays like A.code)");
							word.add(values2[0].trim());
							type.add("String");
							word.add(values2[0].trim());
							type.add("String");				
			}
	
			}
			else if(rq.get(i).trim().startsWith("EN "))
			{

				values = rq.get(i).substring(3,rq.get(i).length()).split(",");

				if(values.length>1){

				for(int k=0; k<values.length; k++){

						String[] values2 = values[k].split("OU");

						if(values2.length>1){

							filtre.append("SELECT id_film from ( ");
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){
									filtre.append("SELECT id_film from FILMS where annee = ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee = ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							if(k!=values.length-1){
								filtre.append(" ) INTERSECT ");
							}
							else{
								filtre.append(" ) ");
							}
						}
						else{
							if(k!=values.length-1){
							filtre.append("SELECT id_film from FILMS where annee = ? INTERSECT ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee = ? ");
						}
							word.add(values[k].trim());
							type.add("Integer");
						}
					}
					}
					else{

						String[] values2 = values[0].split("OU");

						if(values2.length>1){
							filtre.append("SELECT id_film from ( ");
							System.out.println(values2.length);
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){

									filtre.append("SELECT id_film from FILMS where annee = ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee = ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							filtre.append(" ) ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee = ?");
							word.add(values[0].trim());
							type.add("Integer");
						}	
					}
			}
			else if(rq.get(i).trim().startsWith("AVANT "))
			{

				values = rq.get(i).substring(6,rq.get(i).length()).split(",");

				for(int k=0; k<values.length; k++){
					if(k!=values.length-1){

						String[] values2 = values[k].split("OU");
						if(values2.length>1){
							filtre.append("SELECT id_film from ( ");
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){
									filtre.append("SELECT id_film from FILMS where annee < ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee < ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							filtre.append(" ) INTERSECT ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee < ? INTERSECT ");
							word.add(values[k].trim());
							type.add("Integer");
						}

					}
					else{

						String[] values2 = values[k].split("OU");

						if(values2.length>1){
							filtre.append("SELECT id_film from ( ");
							System.out.println(values2.length);
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){

									filtre.append("SELECT id_film from FILMS where annee < ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee < ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							filtre.append(" ) ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee < ?");
							word.add(values[k].trim());
							type.add("Integer");
						}	
					}
				}

			}
			else if(rq.get(i).trim().startsWith("APRES ") || rq.get(i).trim().startsWith("APRÈS "))
			{

				values = rq.get(i).substring(6,rq.get(i).length()).split(",");

				for(int k=0; k<values.length; k++){
					if(k!=values.length-1){

						String[] values2 = values[k].split("OU");
						if(values2.length>1){
							filtre.append("SELECT id_film from ( ");
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){
									filtre.append("SELECT id_film from FILMS where annee > ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee > ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							filtre.append(" ) INTERSECT ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee > ? INTERSECT ");

							word.add(values[k].trim());
							type.add("Integer");
						}

					}
					else{

						String[] values2 = values[k].split("OU");

						if(values2.length>1){
							filtre.append("SELECT id_film from ( ");
							System.out.println(values2.length);
							for(int j=0; j<values2.length; j++){
								if(j!=values2.length-1){

									filtre.append("SELECT id_film from FILMS where annee > ? UNION ");
									word.add(values2[j].trim());
									type.add("Integer");
								}
								else{
									filtre.append("SELECT id_film from FILMS where annee > ?");	
									word.add(values2[j].trim());
									type.add("Integer");
								}
							}
							filtre.append(" ) ");
						}
						else{
							filtre.append("SELECT id_film from FILMS where annee > ?");
							word.add(values[k].trim());
							type.add("Integer");
						}	
					}
				}
			}

			if(cTipart == 2){
				filtre.append(" ) ");
				cTipart =0;
			}

			if(i!=rq.size()-1)
			{
				if(beginOU == true)
					filtre.append(" UNION ");
				if(beginOU == false)
					filtre.append(" INTERSECT ");
			}

			if(i==rq.size()-1 && cTipart==1)
			{
				filtre.append(" ) ");
			}

		}

		System.out.println(word);



		filtre.append(" ) ");

		System.out.println(filtre.toString());


		try{

			
			pstmt = conn.prepareStatement(filtre.toString()+request);


            int compteur = 1;

            for(int i = 0;i<word.size();i++)
            {
			    if(type.get(i).equals("Integer"))
			    {
			    	pstmt.setInt(compteur, Integer.parseInt(word.get(i)));
			    }
			    else{
			    	pstmt.setString(compteur, word.get(i));
			    }

			    compteur+=1;
            }

            rs = pstmt.executeQuery();
         

            json.append("{\"resultat\": [");

            boolean nextValue = rs.next();


            while(nextValue){


            	String titre = rs.getString("titre");
            	int id = rs.getInt("id_film");
            	int annee = rs.getInt("annee");
            	int duree = rs.getInt("duree");
            	String pays = rs.getString("pays");
            	String autreTitre = rs.getString("autre_titres");
            	String prenom = rs.getString("prenom");
            	String nom = rs.getString("nom");
            	String role = rs.getString("role");



            	if(currentTitle == null)
            	{
            		currentTitle = titre;
            		currentOtherTitle = autreTitre;
            		currentAnnee = annee;
            		currentDuree = duree;
            		currentPays = pays;
            		realisateurs = new ArrayList<>();
            		acteurs = new ArrayList<>();

            		if(role.equals("A"))
            		{
            			if(prenom == null){
            				acteurs.add(new NomPersonne(nom,null));
            			}
            			else{
            				acteurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            		if(role.equals("R"))
            		{
            			if(prenom == null){
            				realisateurs.add(new NomPersonne(nom,null));
            			}
            			else{
            				realisateurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            	}
            	else if(currentTitle.equals(titre))
            	{
            		if(role.equals("A"))
            		{
            			if(prenom == null){
            				acteurs.add(new NomPersonne(nom,null));
            			}
            			else{
            				acteurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            		if(role.equals("R"))
            		{
            			if(prenom == null){
            				realisateurs.add(new NomPersonne(nom,null));
            			}
            			else{
            				realisateurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            	}
            	else{

            		autres_titres = new ArrayList<>();

            		if(currentOtherTitle != null)
            		{
            			String[] array = currentOtherTitle.split("\\|");

            			for(int i=0;i<array.length; i++)
            			{
            				autres_titres.add(array[i]);
            			}
            		}

            		InfoFilm info = new InfoFilm(currentTitle, realisateurs, acteurs, currentPays, currentAnnee, currentDuree,autres_titres);

	    			json.append(info.toString());
	    			json.append(",");  

	    			currentTitle = titre;
	    			currentOtherTitle = autreTitre;
            		currentAnnee = annee;
            		currentDuree = duree;
            		currentPays = pays;

            		realisateurs = new ArrayList<>();
            		acteurs = new ArrayList<>();    

            		if(role.equals("A"))
            		{
            			if(prenom == null){
            				acteurs.add(new NomPersonne(nom,"null"));
            			}
            			else{
            				acteurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            		if(role.equals("R"))
            		{
            			if(prenom == null){
            				realisateurs.add(new NomPersonne(nom,"null"));
            			}
            			else{
            				realisateurs.add(new NomPersonne(nom,prenom));
            			}
            		}   		
            	}



	    		if(rs.next())
	    		{

	    		}
	    		else{

            		autres_titres = new ArrayList<>();

            		if(currentOtherTitle != null)
            		{
            			String[] array = currentOtherTitle.split("\\|");

            			for(int i=0;i<array.length; i++)
            			{
            				autres_titres.add(array[i]);
            			}
            		}

            		InfoFilm info = new InfoFilm(currentTitle, realisateurs, acteurs, pays, annee, duree,autres_titres);

	    			json.append(info.toString());

	    			currentTitle = titre;
	    			currentOtherTitle = autreTitre;
            		realisateurs = new ArrayList<>();
            		acteurs = new ArrayList<>();    

            		if(role.equals("A"))
            		{
            			if(prenom == null){
            				acteurs.add(new NomPersonne(nom,"null"));
            			}
            			else{
            				acteurs.add(new NomPersonne(nom,prenom));
            			}
            		}
            		if(role.equals("R"))
            		{
            			if(prenom == null){
            				realisateurs.add(new NomPersonne(nom,"null"));
            			}
            			else{
            				realisateurs.add(new NomPersonne(nom,prenom));
            			}
            		}  
	    		}

            }


			rs.close();
			pstmt.close();

        } catch (SQLException ex) {
              //System.out.println(ex.getMessage());
        }

        json.append("]}");

        return json.toString();
	}


}
