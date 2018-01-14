package cl.uchile.dcc.cli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Pattern pattern = Pattern.compile("^(<[^>]+>)\\s+(<[^>]+>)\\s+(.*)\\s?.$");  
	    Matcher matcher = pattern.matcher("<http1> <http2> \"new york city\" .");
	    if (matcher.matches()) {
	       System.out.println(matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3));
	    } else{
	       // error handling code
	    }
	}

}
