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

@Entity(name = "FACULTADES")
@Table(name = "FACULTADES", schema = "gri")
public class Facultad implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "ID")
	private long id;

	@Column(name = "NOMBRE")
	private String nombre;

	@OneToMany(mappedBy = "facultad", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Centro> centros = new ArrayList<>();

	@OneToMany(mappedBy = "facultad", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Programa> programas = new ArrayList<>();

	public Facultad() {
	}

	public Facultad(long id, String nombre) {
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

	public List<Centro> getCentros() {
		return centros;
	}

	public void setCentros(List<Centro> centros) {
		this.centros = centros;
	}

	public List<Programa> getPrograma() {
		return programas;
	}

	public void setPrograma(List<Programa> programa) {
		this.programas = programa;
	}

}
