package service;

import dto.Game;
import dto.Team;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

public class NflService implements LeagueService{

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

    private static final String currentWeekURL = "http://www.nfl.com/liveupdate/scorestrip/ss.xml";
    // "http://www.nfl.com/liveupdate/scorestrip/postseason/ss.xml" PLAYOFFS
    // The schedule changes on Wednesday 7:00 UTC during the regular season

    private static final String baseURL = "http://www.nfl.com/ajax/scorestrip?season=:PARSE_SEASON&seasonType=:PARSE_TYPE&week=:PARSE_WEEK";

    private Map<String, Team> nflTeamMap = new HashMap();

    @Override
    public Collection<Team> getTeams() {
        return getNFLTeams();
    }

    private Collection<Team> getNFLTeams(){

        URL url;
        URLConnection connection;

        try {
            // "P" = "Pregame"- "H" = "Halftime" - "5" = "Overtime" - "F" = "Final" - "FO" = "Final Overtime";

            String baseUrl = baseURL.replace(":PARSE_TYPE", "REG").replace(":PARSE_SEASON", "2016");

            int week = 1;
            while(true){

                url = new URL(baseUrl.replace(":PARSE_WEEK", String.valueOf(week)));
                connection = url.openConnection();

                Document doc = parseXML(connection.getInputStream());
                Node schedule = doc.getElementsByTagName("ss").item(0);
                Node games = schedule.getFirstChild();

                if(games == null){
                    // No more weeks left
                    break;
                }

                System.out.println("WEEK #"+week);

                NodeList descNodes = games.getChildNodes();

                // Loop through the games
                for (int i = 0; i < descNodes.getLength(); i++) {
                    Node curNode = descNodes.item(i);
                    NamedNodeMap nodeMap = curNode.getAttributes();

                    // Get home team for this game
                    String homeTeamId = nodeMap.getNamedItem("h").getNodeValue();

                    Team homeTeam = getAndAddTeam(homeTeamId);

                    // Get away team for this game
                    String awayTeamId = nodeMap.getNamedItem("v").getNodeValue();
                    Team awayTeam = getAndAddTeam(awayTeamId);

                    String eid = nodeMap.getNamedItem("eid").getNodeValue();
                    String year = eid.substring(0, 3);
                    String month = eid.substring(4, 6);
                    String day = eid.substring(6, 8);

                    String dayLong = nodeMap.getNamedItem("d").getNodeValue();
                    String time = nodeMap.getNamedItem("t").getNodeValue();

                    String[] timeParts = time.split(":");

                    int hour = Integer.parseInt(timeParts[0]);

                    if(hour < 9){
                        hour += 12;
                    }

                    Date gameDate = sdf.parse(eid.substring(0, 8) + " " + hour + ":" + timeParts[1]);

                    // Home Team
                    Game homeGame = new Game();

                    homeGame.setTime(gameDate);
                    homeGame.setHomeTeam(true);
                    homeGame.setOpponentTeamId(awayTeamId);

                    homeTeam.getSchedule().add(homeGame);

                    // Away Team
                    Game awayGame = new Game();

                    awayGame.setTime(gameDate);
                    awayGame.setHomeTeam(false);
                    awayGame.setOpponentTeamId(homeTeamId);

                    awayTeam.getSchedule().add(awayGame);

                    System.out.println(awayGame.getOpponentTeamId() + " vs " + homeGame.getOpponentTeamId());
                }

                // If list size isn't the week size, team has a bye week
                /*for(TeamObj team : teams.values()){
                    if(team.getSchedule().size() < week){
                        GameObj game = new GameObj();
                        game.setOpponentMascot("Bye");
                        team.getSchedule().add(game);
                    }
                }*/

                week++;

            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return nflTeamMap.values();
    }

    private Team getAndAddTeam(String teamId){
        Team team = nflTeamMap.get(teamId);
        if(team == null){
            team = new Team();
            team.setId(teamId);
            team.setLeagueId("NFL");
            nflTeamMap.put(teamId, team);
        }
        return team;
    }

    private static Document parseXML(InputStream stream) throws Exception {

        DocumentBuilderFactory objDocumentBuilderFactory;
        DocumentBuilder objDocumentBuilder;
        Document doc;
        try {
            objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
            objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();

            doc = objDocumentBuilder.parse(stream);
        }
        catch(Exception ex) {
            throw ex;
        }

        return doc;
    }

}