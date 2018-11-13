package co.edu.uniquindio.gri.Control;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.Produces;

public class Recurso {

	private static EntityManagerFactory factory;

	@Produces
	public static EntityManager getEntityManager() {
		factory = Persistence.createEntityManagerFactory("Persistencia");
		EntityManager manager = factory.createEntityManager();
		return manager;
	}
}
