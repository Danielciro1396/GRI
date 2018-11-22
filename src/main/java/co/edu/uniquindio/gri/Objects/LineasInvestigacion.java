package co.edu.uniquindio.gri.Objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import co.edu.uniquindio.gri.CvLac.Investigador;
import co.edu.uniquindio.gri.GrupLac.Grupo;

@Entity(name = "LINEASINVESTIGACION")
@Table(name = "LINEASINVESTIGACION", schema = "gri")
public class LineasInvestigacion implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private long id;

	@Column(name = "NOMBRE", length = 400)
	private String nombre;

	@ManyToMany(mappedBy = "lineasInvestigacion", cascade = CascadeType.MERGE)
	private List<Investigador> investigadores = new ArrayList<Investigador>();

	@ManyToMany(mappedBy = "lineasInvestigacion", cascade = CascadeType.MERGE)
	private List<Grupo> grupos = new ArrayList<Grupo>();

	public LineasInvestigacion() {
	}

	public LineasInvestigacion(long id, String nombre) {
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

	public List<Grupo> getGrupos() {
		return grupos;
	}

	public void setGrupos(List<Grupo> grupos) {
		this.grupos = grupos;
	}

	public List<Investigador> getInvestigadores() {
		return investigadores;
	}

	public void setInvestigadores(List<Investigador> investigadores) {
		this.investigadores = investigadores;
	}

}