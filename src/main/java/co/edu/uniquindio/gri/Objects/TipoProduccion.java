package co.edu.uniquindio.gri.Objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import co.edu.uniquindio.gri.CvLac.Produccion;

@Entity(name = "TIPOPRODUCCION")
@Table(name = "TIPOPRODUCCION", schema = "gri")
public class TipoProduccion implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private long id;

	@Column(name = "NOMBRE", length = 100)
	private String nombre;

	@OneToMany(mappedBy = "tipoProduccion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Produccion> produccion = new ArrayList<Produccion>();

	@OneToMany(mappedBy = "tipoProduccion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<co.edu.uniquindio.gri.GrupLac.Produccion> produccionG= new ArrayList<co.edu.uniquindio.gri.GrupLac.Produccion>();

	public TipoProduccion() {
	}

	public TipoProduccion(long id, String nombre) {
		this.id = id;
		this.nombre = nombre;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public List<Produccion> getProduccion() {
		return produccion;
	}

	public void setProduccion(List<Produccion> produccion) {
		this.produccion = produccion;
	}

	public List<co.edu.uniquindio.gri.GrupLac.Produccion> getProduccionG() {
		return produccionG;
	}

	public void setProduccionG(List<co.edu.uniquindio.gri.GrupLac.Produccion> produccionG) {
		this.produccionG = produccionG;
	}

}
