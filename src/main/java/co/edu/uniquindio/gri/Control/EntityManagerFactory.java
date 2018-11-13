package co.edu.uniquindio.gri.Control;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class EntityManagerFactory {

	private static final long serialVersionUID = 1L;
	@Inject
	private Recurso recurso;

	public EntityManager getEM() {
		return recurso.getEntityManager();
	}

}
