package co.edu.uniquindio.gri.Master;

import co.edu.uniquindio.gri.GrupLac.GrupLac;

public class GRI {

	public static void main(String[] args) {
		
		long startTime = System.currentTimeMillis();
		long stopTime = 0;
		long elapsedTime = 0;
		
		GrupLac grupLac = new GrupLac();
		
		grupLac.eliminarDatos();
		
		grupLac.guardarDatos(grupLac.scrapData());		
		
		stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
		System.err.println(elapsedTime);
		
	}

}
