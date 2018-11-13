package co.edu.uniquindio.gri.CvLac;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import co.edu.uniquindio.gri.GrupLac.Grupo;
import co.edu.uniquindio.gri.Objects.LineasInvestigacion;

@Entity(name = "INVESTIGADORES")
@Table(name = "INVESTIGADORES", schema = "gri")
public class Investigador implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private long id;

	@Column(name = "NOMBRE", length = 200)
	private String nombre;

	@Column(name = "CATEGORIA", length = 200)
	private String categoria;

	@Column(name = "NIVELACADEMICO", length = 200)
	private String nivelAcademico;

	@OneToMany(mappedBy = "investigador", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Idiomas> idiomas = new ArrayList<Idiomas>();

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "INVEST_LINEAS", joinColumns = { @JoinColumn(name = "INVESTIGADORES_ID") }, inverseJoinColumns = {
			@JoinColumn(name = "LINEASINVESTIGACION_ID") }, schema = "gri")
	private List<LineasInvestigacion> lineasInvestigacion = new ArrayList<LineasInvestigacion>();

	@OneToMany(mappedBy = "investigador", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Produccion> producciones = new ArrayList<Produccion>();

	@OneToMany(mappedBy = "investigador", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProduccionBibliografica> produccionesBibliograficas = new ArrayList<ProduccionBibliografica>();

	@ManyToMany(mappedBy = "investigadores", cascade = CascadeType.ALL)
	private List<Grupo> grupos = new ArrayList<Grupo>();

	public Investigador(long id, String nombre, String categoria, String nivelAcademico, List<Idiomas> idiomas,
			List<LineasInvestigacion> lineasInvestigacion, List<Produccion> producciones,
			List<ProduccionBibliografica> produccionesBibliograficas) {
		this.id = id;
		this.nombre = nombre;
		this.categoria = categoria;
		this.nivelAcademico = nivelAcademico;
		this.idiomas = idiomas;
		this.lineasInvestigacion = lineasInvestigacion;
		this.produccionesBibliograficas = produccionesBibliograficas;

	}

	public Investigador() {
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

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public String getNivelAcademico() {
		return nivelAcademico;
	}

	public void setNivelAcademico(String nivelAcademico) {
		this.nivelAcademico = nivelAcademico;
	}

	public List<Idiomas> getIdiomas() {
		return idiomas;
	}

	public void setIdiomas(List<Idiomas> idiomas) {
		this.idiomas = idiomas;
	}

	public List<Produccion> getProducciones() {
		return producciones;
	}

	public void setProducciones(List<Produccion> producciones) {
		this.producciones = producciones;
	}

	public List<ProduccionBibliografica> getProduccionesBibliograficas() {
		return produccionesBibliograficas;
	}

	public void setProduccionesBibliograficas(List<ProduccionBibliografica> produccionesBibliograficas) {
		this.produccionesBibliograficas = produccionesBibliograficas;
	}

	public List<LineasInvestigacion> getLineasInvestigacion() {
		return lineasInvestigacion;
	}

	public void setLineasInvestigacion(List<LineasInvestigacion> lineasInvestigacion) {
		this.lineasInvestigacion = lineasInvestigacion;
	}

	public List<Grupo> getGrupos() {
		return grupos;
	}

	public void setGrupos(List<Grupo> grupos) {
		this.grupos = grupos;
	}

	public void removeLineasInvestigacion(LineasInvestigacion lineas) {
		lineasInvestigacion.remove(lineas);
		lineas.getInvestigadores().remove(this);
	}
}
