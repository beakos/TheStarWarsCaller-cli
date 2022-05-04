import java.util.Scanner;
public class main {
    public static void main (String[] args) {
        
        /* Star War films part of the main storyline. */
        episode one = new episode();
        one.movieName = "Star Wars";
        one.episode = 1;
        one.title = "The Phantom Menace";
        one.year = 1999;
        one.information = "Jedi master with apprentice Obi One Konobi, find this kid named Anakin blah blah and fight boss at the end....";

        episode two = new episode();
        two.movieName = "Star Wars";
        two.episode = 2;
        two.title = "Attack of the Clones";
        two.year = 2002;
        two.information = "Anakin grows up and is trained by his master Obi Won";

        episode three = new episode();
        three.movieName = "Star Wars";
        three.episode = 3;
        three.title = "Revenge of the Sith";
        three.year = 2005;
        three.information = "Anakin becomes Vader";
        three.raiting = 9.5;

        episode four = new episode();
        four.movieName = "Star Wars";
        four.episode = 4;
        four.title = "A New Hope";
        four.year = 1977;
        four.information = "Darth Vader makes an apperance after suffering";
        four.raiting = 8.0;

        episode five = new episode();
        five.movieName = "Star Wars";
        five.episode = 5;
        five.title = "The Empire Strikes Back";
        five.year = 1980;
        five.information = "Lorum Ipsum";
        five.raiting = 9.0;

        episode six = new episode();
        six.movieName = "Star Wars";
        six.episode = 6;
        six.title = "Return of the Jedi";
        six.year = 1983;
        six.information = "Lorum Ipsum..";

        episode seven = new episode();
        seven.movieName = "Star Wars";
        seven.episode = 7;
        seven.title = "The Force Awakens";
        seven.year = 2015;
        seven.information = "Lorum Ipsum";

        /* Insert Episode Details 8 and 9 here --- remove this comment when ommitted!! */

        /* Star War's Movies not part of the main storyline aka stories. */
        stories rouge = new stories();
        rouge.movieName = "Rouge One: ";
        rouge.title = "A Star Wars Story";
        rouge.year = 2016;
        rouge.information = "Lorum Ipsum..";

        stories solo = new stories();
        solo.movieName = "Solo: ";
        solo.title = "A Star Wars Story";
        solo.year = 2018;
        solo.information = "Lorum Ipsum";

        /* Star War's TV Shows related to the Star Wars Franchise. */
        shows mandalorian = new shows();
        mandalorian.title = "The Mandalorian";

        /* Star War's Games related to the Star Wars Franchise */
        games battlefront = new games();
        battlefront.title = "Battlefront";
        battlefront.year = 2003;

        //Scanner keyboard = new Scanner(System.in);


        /* Scanner keyboard = new Scanner(System.in);
        System.out.print("Show Episode Information:(Choose 0 to list all. Choose 1 - 11 to list episode name): ");

        //identify all classes here

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
        } */

    }
}