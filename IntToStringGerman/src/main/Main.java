package main;	// wird ben�tigt, um module-info nutzen zu k�nnen

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpRequest.BodyProcessor;
import jdk.incubator.http.HttpResponse;
import jdk.incubator.http.HttpResponse.BodyHandler;

/**
 * Mit dieser Klasse kann man Zahlen in ihre deutsche 
 * String Version umwandeln.
 * @author Emre Kara
 */
public class Main {
	
	/**
	 * Normale Namen der einzelnen Zahlen ohne Schnick Schnack
	 */
	private static String[] singleNumbersName = new String[] {
			"", "eins", "zwei", "drei", "vier", "f�nf", "sechs", "sieben", "acht", "neun"
	};
	
	/**
	 * Suffix der Zahlen, wenn sie im Zehner Bereich sind und 0 am Ende haben.
	 * z.B. 20 -> zwanZIG, 30 -> drei�IG, 100 -> hundert 
	 */
	private static String[] tenSuffix = new String[] {
			"", "", "zig", "�ig", "zig", "zig", "zig", "zig", "zig", "zig"
	};
	
	/**
	 * Namen der Block-Nummern/Level (Block-Nummer = (int)((Anzahl Zeichen-1) / 3)) im Singular (z.B. 1 Million).
	 * Hierbei haben z.B. 0-999 die Block-Nummer 0, 1000-999999 die Block-Nummer 1 usw. 
	 */
	private static String[] blockNumberSingleNames = new String[] {
		"", "tausend", " Million ", " Milliarde ", " Billion ", " Billiarde "
	};
	
	/**
	 * Namen der Block-Nummern/Level (Block-Nummer = (int)((Anzahl Zeichen-1) / 3)) im Plural.
	 * Hierbei haben z.B. 0-999 die Block-Nummer 0, 1000-999999 die Block-Nummer 1 usw. 
	 */
	private static String[] blockNumberPluralNames = new String[] {
		"", "tausend", " Millionen ", " Milliarden ", " Billionen ", " Billiarden "
	};
	
	/**
	 * Name der vollen Zahl bei Zehnern (z.B. zehn statt einszehn)
	 */
	private static Map<Integer, String> changedNumbersNameTen = new HashMap<Integer, String>();
	
	/**
	 * Name der Zahl, wenn sie ein Zehner ist (z.B. zwanzig statt zweizig)
	 */
	private static Map<Integer, String> changedNumbersNameTenWithoutRest = new HashMap<Integer, String>();
	
	/**
	 * Initialisiere die Einstellungen f�r die Maps.
	 * Sollte vor Nutzung der Methoden ausgef�hrt werden!
	 */
	private static void InitMaps() {		
		changedNumbersNameTen.put(10, "zehn");
		changedNumbersNameTen.put(11, "elf");
		changedNumbersNameTen.put(12, "zw�lf");
		changedNumbersNameTen.put(16, "sechzehn");
		changedNumbersNameTen.put(17, "siebzehn");
		
		changedNumbersNameTenWithoutRest.put(2, "zwan");
		changedNumbersNameTenWithoutRest.put(6, "sech");
		changedNumbersNameTenWithoutRest.put(7, "sieb");
	}
	
	/**
	 * Bekomme ein short-Array von x-Ziffer-Bl�cken einer Zahl.
	 * z.B. 0-999 geh�rt zu Block 0, 1000-999999 geh�rt zu Block 1 usw.
	 * @param number Die Zahl, die wir in einen String umwandeln m�chten
	 * @param digits Die Variable f�r x (im Programm nur 3)
	 * @return short-Array der Bl�cke, beginnend von hinten
	 */
	private static short[] getBlocks(long number, int digits) {
		String numberstr = Long.toString(number);  
		int strlength = numberstr.length();
		int blocksamount = (strlength-1)/digits;
		short[] blocks = new short[blocksamount+1];
		for (int i = 0; i < strlength; i+= digits) {
			// die Zahl in digits-Anzahl Zeichen-Bl�cke unterteilen
			blocks[i/digits] = Short.parseShort(numberstr.substring(Math.max(strlength-i-digits, 0), strlength-i));
		}
		return blocks;
	}
	
	
	/**
	 * F�ge die String Darstellung eines Blocks in den StringBuilder.
	 * Der StringBuilder wird f�r bessere Performance benutzt, da Strings immutable sind.
	 * @param builder StringBuilder
	 * @param threedigitblock Einer der Bl�cke
	 * @param blocknumber Level des Blocks (0 f�r 0-999, 1 f�r 1000-999999 usw.)
	 */
	private static void addBlocksStringRepresentation(StringBuilder builder, int digitblock, int blocknumber) {
		// speichern, damit man sp�ter unterscheiden kann, ob die Zahl z.B. 101 oder 1 ist
		boolean wasatleasthundred = false;	
		
		// String-Darstellung f�r 100er bekommen und String auf Zehner k�rzen
		if (digitblock >= 100) {
			if (digitblock/100 == 1) {
				builder.append("ein");
			} else {
				builder.append(singleNumbersName[digitblock/100]);
			}
			builder.append("hundert");
			wasatleasthundred = true;
			digitblock %= 100;
		}
		
		// Volle Zehner-Zahl kann umgewandelt werden? (z.B. elf, zw�lf usw.)
		if (changedNumbersNameTen.containsKey(digitblock)) {
			builder.append(changedNumbersNameTen.get(digitblock));
		} else {	// m�ssen Einser auch beachten
			
			// Beachte die Einer-Zahl bei Zehner (z.B. einundzwanzig statt einsundzwanzig) 
			// ODER bei Einer mit tausend (eintausend statt einstausend)
			if ((digitblock >= 10 || blocknumber == 1) && digitblock % 10 == 1) {
				builder.append("ein");
				
			// Beachte die Einer-Zahl (nicht im Hundert gewesen) vor allen Block-Namen ab Million
			// eine Million statt eins Million
			} else if (blocknumber >= 2 && digitblock == 1 && !wasatleasthundred) {
				builder.append("eins");
				
			// Ansonsten die Einer-Zahl alleine hinzuf�gen
			} else {
				builder.append(singleNumbersName[digitblock%10]);
			}
			
			// Bei 13 bis 19 ist es immer (au�er in Ausnahmef�llen) Zahlen-Name + "zehn"
			if (digitblock > 12 && digitblock < 20) {
				builder.append("zehn");
				
			// Bei allen Zehnern, die bisher nicht ber�cksichtigt wurden
			} else if (digitblock > 10) {
				// Wenn es eine Einser-Zahl gibt, die nicht 0 ist, "und" hinzuf�gen 
				// (z.B. einUNDzwanzig, aber nicht zwanzig)
				if (digitblock % 10 != 0) {
					builder.append("und");
				}
				
				// Bei speziellen Zehner-Zahlen, wie z.B. ZWANzig statt ZWEIzig
				if (digitblock >= 10 && changedNumbersNameTenWithoutRest.containsKey(digitblock/10)) {
					builder.append(changedNumbersNameTenWithoutRest.get(digitblock/10));
					
				// Ansonsten einfach die Zehnerzahl hinzuf�gen (z.B. DREI�ig, hier nur DREI)
				} else {
					builder.append(singleNumbersName[digitblock/10]);
				}
				
				// Suffix vom Zehner hinzuf�gen, z.B. "�ig", bei Drei�ig
				builder.append(tenSuffix[digitblock/10]);
			}		
			
		}
	}
	
	/**
	 * Die String Darstellung einer Zahl.
	 * @param number Die Zahl.
	 * @return Die String Darstellung in Deutsch
	 */
	private static String getStringRepresentation(long number) {
		// Sonderfall - nur 0 wird
		if (number == 0) 
			return "null";
		short[] blocks = getBlocks(number, 3);
		StringBuilder builder = new StringBuilder();
		for (int level = blocks.length - 1; level >= 0; --level) {
			if (blocks[level] != 0) {
				addBlocksStringRepresentation(builder, blocks[level], level);
				if (blocks[level] == 1)
					builder.append(blockNumberSingleNames[level]);
				else 
					builder.append(blockNumberPluralNames[level]);
			}
		}
		return builder.toString().trim();
	}
	
	/**
	 * Die Test-Methode, um die Korrektheit zu checken.
	 * Hierbei wird die Website "zahlen-ausschreiben.de" genutzt.
	 * Da anscheinend kein Spam-Schutz existiert, k�nnen die Tests recht schnell ausgef�hrt werden.
	 * @param number Die zu testende Nummer.
	 */
	private static void tryMethod(long number) {
		try {
			String str = getStringRepresentation(number);

			HttpClient client = HttpClient.newHttpClient();
			
			HttpResponse<String> response = client.send(
					HttpRequest
						.newBuilder(URI.create("https://zahlen-ausschreiben.de/convert.php"))
						.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
						.POST(BodyProcessor.fromString("number="+number+"&format=undefined"))
						.build()
					, BodyHandler.asString(StandardCharsets.UTF_8));
			
			String result = response.body().trim();
		    
			if (!str.equals(result))
				System.out.println("[ERROR] Different result: Should be '"+result+"', but is '"+str+"'");
			else
				System.out.println("Checked number: "+number+ " - '"+str+"'");
			
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	public static void main(String[] args) {
		InitMaps();  // MUSS am Anfang ausgef�hrt werden!
		
		// Optionaler Test, der einfach deaktiviert oder ver�ndert werden kann
		for (long number = 1; number < Long.MAX_VALUE; number += 1030) 
			tryMethod(number);
	}
}
