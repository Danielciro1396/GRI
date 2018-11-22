package co.edu.uniquindio.gri.Objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import co.edu.uniquindio.gri.CvLac.Produccion;
import co.edu.uniquindio.gri.CvLac.ProduccionBibliografica;

@Entity(name = "TIPOS")
@Table(name = "TIPOS", schema = "gri")
public class Tipo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private long id;

	@Column(name = "NOMBRE", length = 100)
	private String nombre;

	@OneToMany(mappedBy = "tipo", cascade = CascadeType.MERGE)
	private List<Produccion> produccion = new ArrayList<Produccion>();

	@OneToMany(mappedBy = "tipo", cascade = CascadeType.MERGE)
	private List<ProduccionBibliografica> produccionBibliografica = new ArrayList<ProduccionBibliografica>();

	@OneToMany(mappedBy = "tipo", cascade = CascadeType.MERGE)
	private List<co.edu.uniquindio.gri.GrupLac.Produccion> produccionG = new ArrayList<co.edu.uniquindio.gri.GrupLac.Produccion>();

	@OneToMany(mappedBy = "tipo", cascade = CascadeType.MERGE)
	private List<co.edu.uniquindio.gri.GrupLac.ProduccionBibliografica> produccionBibliograficaG = new ArrayList<co.edu.uniquindio.gri.GrupLac.ProduccionBibliografica>();

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "TIPOPRODUCCION_ID")
	private TipoProduccion tipoProduccion;
	
	public Tipo() {
	}

	public Tipo(long id, String nombre, TipoProduccion tipoProduccion) {
		this.id = id;
		this.nombre = nombre;
		this.tipoProduccion = tipoProduccion;
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

	public List<ProduccionBibliografica> getProduccionBibliografica() {
		return produccionBibliografica;
	}

	public void setProduccionBibliografica(List<ProduccionBibliografica> produccionBibliografica) {
		this.produccionBibliografica = produccionBibliografica;
	}

	public List<co.edu.uniquindio.gri.GrupLac.ProduccionBibliografica> getProduccionBibliograficaG() {
		return produccionBibliograficaG;
	}

	public void setProduccionBibliograficaG(
			List<co.edu.uniquindio.gri.GrupLac.ProduccionBibliografica> produccionBibliograficaG) {
		this.produccionBibliograficaG = produccionBibliograficaG;
	}

	public TipoProduccion getTipoProduccion() {
		return tipoProduccion;
	}

	public void setTipoProduccion(TipoProduccion tipoProduccion) {
		this.tipoProduccion = tipoProduccion;
	}

}
