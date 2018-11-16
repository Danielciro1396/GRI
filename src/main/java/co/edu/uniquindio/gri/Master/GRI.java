package co.edu.uniquindio.gri.Master;

import co.edu.uniquindio.gri.CvLac.CvLac;

public class GRI {

	public static void main(String[] args) {
//		GrupLac grupLac = new GrupLac();
//		
//		grupLac.guardarDatos(grupLac.scrapData());		
		
		CvLac cvlac = new CvLac();
		
		cvlac.extraer("http://scienti.colciencias.gov.co:8081/cvlac/visualizador/generarCurriculoCv.do?cod_rh=0000884910", "ACTUAL");
		
//		grupLac.extraer("https://scienti.colciencias.gov.co/gruplac/jsp/visualiza/visualizagr.jsp?nro=00000000002591");
		
//		grupLac.extraer("https://scienti.colciencias.gov.co/gruplac/jsp/visualiza/visualizagr.jsp?nro=00000000002595");

	
	}

}
