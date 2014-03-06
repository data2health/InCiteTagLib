package edu.uiowa.slis.incite;

import edu.uiowa.util.Generator;
import edu.uiowa.util.GeneratorFactory;

public class ConnectionFactory extends GeneratorFactory {

	public Generator newInstance() {
		try {
			return new ConnectionGenerator(HTMLCrawler.getConnection());
		} catch (Exception e) {
			return null;
		}
	}
	
}
