package co.edu.uniquindio.gri.Master;

import co.edu.uniquindio.gri.CvLac.CvLac;
import co.edu.uniquindio.gri.GrupLac.GrupLac;

public class GRI {

	public static void main(String[] args) {
		GrupLac grupLac = new GrupLac();
		
		grupLac.guardarDatos(grupLac.scrapData());		
		
//		CvLac cvlac = new CvLac();
//		
//		cvlac.extraer("http://scienti.colciencias.gov.co:8081/cvlac/visualizador/generarCurriculoCv.do?cod_rh=0000884910", "ACTUAL");
		

	}

}
