package co.edu.uniquindio.gri.Master;

import co.edu.uniquindio.gri.GrupLac.GrupLac;

public class GRI {

	public static void main(String[] args) {
		GrupLac grupLac = new GrupLac();
		
		grupLac.eliminarDatos();
		
		grupLac.guardarDatos(grupLac.scrapData());		
		
	}

}
