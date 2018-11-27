package co.edu.uniquindio.gri.Objects;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import co.edu.uniquindio.gri.CvLac.Investigador;
import co.edu.uniquindio.gri.GrupLac.Grupo;
 
@Entity(name = "GRUPOS_INVES")
@Table(name = "GRUPOS_INVES", schema = "gri")
public class GruposInves implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private long id;
	
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "GRUPOS_ID")
	private Grupo grupo;
 
	@ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "INVESTIGADORES_ID")
    private Investigador investigador;
    
    @Column(name = "ESTADO", length = 50)
	private String estado;
 
	public GruposInves() {
		super();
	}

	public GruposInves(Grupo grupo, Investigador investigador, String estado) {
		this.grupo = grupo;
		this.investigador = investigador;
		this.estado = estado;
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Grupo getGrupo() {
		return grupo;
	}

	public void setGrupo(Grupo grupo) {
		this.grupo = grupo;
	}

	public Investigador getInvestigador() {
		return investigador;
	}

	public void setInvestigador(Investigador investigador) {
		this.investigador = investigador;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}
    	
}
