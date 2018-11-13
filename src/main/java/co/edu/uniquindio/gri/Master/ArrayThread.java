package co.edu.uniquindio.gri.Master;

import co.edu.uniquindio.gri.GrupLac.GrupLac;
import co.edu.uniquindio.gri.GrupLac.Grupo;

public class ArrayThread implements Runnable {

	public String url;
	public int inicio;
	GrupLac grupLac;
	Grupo grupo;
	
	public ArrayThread(String url, int inicio, GrupLac grupLac,Grupo grupo) {
		this.url = url;
		this.inicio = inicio;
		this.grupLac = grupLac;
		this.grupo=grupo;

	}

	/**
	 * Metodo que ejecuta cada hilo, y hace un llamado a las clases que hacen la
	 * estraccion de los datos
	 */
	@Override
	public void run() {

		grupLac.extraer(url, grupo);

	}

}
