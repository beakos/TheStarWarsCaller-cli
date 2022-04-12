import java.util.Scanner;

public class StarWarsMovieDetails {
    public static void main (String[] args) {
        Scanner keyboard = new Scanner(System.in);
        System.out.print("Show Episode Information:(Choose 0 to list all. Choose 1 - 11 to list episode name): ");

        int mov = keyboard.nextInt(); // Movies
        String ep1 = "Star Wars: Episode I - The Phantom Menace (1999)";
        String ep2 = "Star Wars: Episode II - Attack of the Clones (2002)";
        String ep3 = "Star Wars: Episode III - Revenge Of The Sith (2005)";
        String ep4 = "Solo: A Star Wars Story (2018)";
        String ep5 = "Rogue One: A Star Wars Story (2016)";
        String ep6 = "Star Wars: Episode IV - A New Hope (1977)";
        String ep7 = "Star Wars: Episode V - The Empire Strikes Back (1980)";
        String ep8 = "Star Wars: Episode VI - Return of the Jedi (1983)";
        String ep9 = "Star Wars: Episode VII - The Force Awakens (2015)";
        String ep10 = "Star Wars: Episode VIII - The Last Jedi (2017)";
        String ep11 = "Star Wars: Episode IX - The Rise of Skywalker (2019)";
        


        if (mov == 1) {
            System.out.println(ep1);
            System.out.println(" ");
            System.out.println("Disney+ Link: https://www.disneyplus.com/movies/star-wars-the-phantom-menace-episode-i/2ezYynkgW1AH");
            System.out.println(" ");
            System.out.println(" ");
            System.out.println("Bio: ");
            System.out.println("Experience the heroic action and unforgettable adventures of Star Wars Episode I: The Phantom Menace and see the first fateful steps in the journey of Anakin Skywalker. Stranded on the desert planet Tatooine after rescuing young Queen Amidala from the impending invasion of Naboo, Jedi apprentice Obi-Wan Kenobi and his Jedi Master Qui-Gon Jinn discover nine-year-old Anakin, a young slave unusually strong in the Force. Anakin wins a thrilling Podrace — and with it his freedom as he leaves his home to be trained as a Jedi. The heroes return to Naboo, where Anakin and the Queen face massive invasion forces, while the two Jedi contend with the deadly foe Darth Maul. Only then do they realize the invasion is merely the first step in a sinister scheme by the reemergent forces of darkness known as the Sith.");

        } else if (mov == 2) {
            System.out.println(ep2);

        } else if (mov == 3) {
            System.out.println(ep3);

        } else if (mov == 4) {
            System.out.println(ep4);

        } else if (mov == 5) {
            System.out.println(ep5);

        } else if (mov == 6) {
            System.out.println(ep6);

        } else if (mov == 7) {
            System.out.println(ep7);

        } else if (mov == 8) {
            System.out.println(ep8);

        } else if (mov == 9) {
            System.out.println(ep9);

        } else if (mov == 10) {
            System.out.println(ep10);

        } else if (mov == 11) {
            System.out.println(ep11);

        } else if (mov == 0) {
            System.out.println(" ");
            System.out.println("--------------------------------");
            System.out.println("--------------------------------");
            
            System.out.println("Film One: " + ep1);
            System.out.println(" ");
            
            System.out.println("Film Two: " + ep2);
            System.out.println(" ");
            
            System.out.println("Film Three: " + ep3);
            System.out.println(" ");
            
            System.out.println("Film Four: " + ep4);
            System.out.println(" ");
            
            System.out.println("Film Five: " + ep5);
            System.out.println(" ");
            
            System.out.println("Film Six: " + ep6);
            System.out.println(" ");
            
            System.out.println("Film Seven: " + ep7);
            System.out.println(" ");
            
            System.out.println("Film Eight: " + ep8);
            System.out.println(" ");
            
            System.out.println("Film Nine: " + ep9);
            System.out.println(" ");
            
            System.out.println("Film Ten: " + ep10);
            System.out.println(" ");
            
            System.out.println("Film Eleven: " + ep11);
        }

    }
}