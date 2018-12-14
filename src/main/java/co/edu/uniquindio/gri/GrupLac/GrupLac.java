package co.edu.uniquindio.gri.GrupLac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import co.edu.uniquindio.gri.CvLac.CvLac;
import co.edu.uniquindio.gri.CvLac.Investigador;
import co.edu.uniquindio.gri.Master.ArrayThread;
import co.edu.uniquindio.gri.Master.Constantes;
import co.edu.uniquindio.gri.Objects.GruposInves;
import co.edu.uniquindio.gri.Objects.LineasInvestigacion;
import co.edu.uniquindio.gri.Objects.Tipo;
import co.edu.uniquindio.gri.Objects.TipoProduccion;

public class GrupLac {

	List<String> urlSet = Collections.synchronizedList(new ArrayList<String>());

	public EntityManagerFactory emf = Persistence.createEntityManagerFactory("Persistencia");

	public EntityManager manager = emf.createEntityManager();

	// Nombre del grupo de investigacion
	String nombreGrupo = "";

	/*
	 * Lista sincronizada en la que se guardan todos los grupos junto a su
	 * respectiva informacion
	 */
	List<Grupo> grupos = Collections.synchronizedList(new ArrayList<Grupo>());

	/*
	 * Lista sincronizada en la que se guardan todos los grupos junto a su
	 * respectiva informacion
	 */
	List<Grupo> gruposInicial = Collections.synchronizedList(new ArrayList<Grupo>());

	public List<GruposInves> grupoInves = Collections.synchronizedList(new ArrayList<GruposInves>());

	/**
	 * Este metodo se encarga de hacer el llamado al metodo que lee un archivo plano
	 * y carga el dataSet de url's, ademas, crea y lanza un pool de hilos para
	 * mejorar el tiempo de ejecucion del programa
	 */
	public List<Grupo> scrapData() {

		gruposInicial = leerDataSet();
		ExecutorService executor = Executors.newFixedThreadPool(30);
		for (int i = 0; i < urlSet.size(); i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Runnable worker = new ArrayThread(urlSet.get(i), i, this, gruposInicial.get(i));
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}
		
		return grupos;
	}

	/**
	 * 
	 */
	public List<Grupo> leerDataSet() {

		urlSet = new ArrayList<String>();

		@SuppressWarnings("unchecked")
		List<Grupo> grupos = (ArrayList<Grupo>) manager.createQuery("FROM GRUPOS").getResultList();
		for (int i = 0; i < grupos.size(); i++) {
			String cadena = "00000000000000" + grupos.get(i).getId();
			cadena = cadena.substring(cadena.length() - Constantes.LINK_GRUPLAC, cadena.length());
			String url = "https://scienti.colciencias.gov.co/gruplac/jsp/visualiza/visualizagr.jsp?nro=" + cadena;
			urlSet.add(url);
		}
		return grupos;
	}

	/**
	 * Método provisto por JSoup para comprobar el Status code de la respuesta que
	 * recibo al hacer la petición Codigos: 200 OK 300 Multiple Choices 301 Moved
	 * Permanently 305 Use Proxy 400 Bad Request 403 Forbidden 404 Not Found 500
	 * Internal Server Error 502 Bad Gateway 503 Service Unavailable
	 * 
	 * @param url,
	 *            el enlace de la página web a analizar.
	 * @return Status Code, el código que identifica el estado de la página.
	 */
	@SuppressWarnings("deprecation")
	public int getStatusConnectionCode(String url) {

		Response response = null;

		try {
			response = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(0).validateTLSCertificates(false)
					.ignoreHttpErrors(true).execute();
		} catch (IOException ex) {

			return getStatusConnectionCode(url);
		}
		return response.statusCode();
	}

	/**
	 * Método que retorna un objeto de la clase Document con el contenido del HTML
	 * de la web para poder ser parseado posteriormente con JSoup
	 * 
	 * @param url,
	 *            el enlace de la página web a analizar.
	 * @return Documento con el HTML de la página en cuestión.
	 */
	@SuppressWarnings("deprecation")
	public Document getHtmlDocument(String url) {

		Document doc = null;
		try {
			doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(0).validateTLSCertificates(false).get();
		} catch (IOException ex) {
			return getHtmlDocument(url);
		}
		return doc;
	}

	/**
	 * Metodo que realiza la extraccion de la estructura de una pagina web, separa
	 * en las diferentas categorias la estructura de la pagina web y hacer el
	 * llamado al metodo que asigna los datos a cada grupo
	 * 
	 * @param url,
	 *            direccion url de un investigador
	 */
	public void extraer(String url, Grupo grupo) {

		if (getStatusConnectionCode(url) == 200) {

			// Datos Generales
			ArrayList<String> elemInfoGeneral = new ArrayList<>();

			// Se obtiene el HTML de la web en un objeto Document
			Document document = getHtmlDocument(url);

			// Busca todas las coincidencias que estan dentro de las caracteristicas
			// indicadas
			Elements entradas = document.select("body>table");

			// Busca las coincidencias para poder extraer el nombre del grupo de
			// investigacion
			Elements entradas2 = document.select("span.celdaEncabezado");

			/*
			 * Extraer Nombre del grupo de investigacion
			 */

			for (Element elem : entradas2) {
				if (elem.toString().contains("span class")) {
					nombreGrupo = elem.text().toUpperCase();
				}
			}

			for (Element elem : entradas) {

				/*
				 * Extraer Datos Generales del grupo
				 */

				if (elem.text().startsWith("Datos básicos")) {
					elemInfoGeneral.add(elem.toString());
					elemInfoGeneral = limpiar(elemInfoGeneral);
				}

				if (elem.text().startsWith("Líneas de investigación declaradas por el grupo")) {
					ArrayList<String> aux = new ArrayList<>();
					aux.add(elem.toString());
					aux = limpiar(aux);
					elemInfoGeneral.addAll(aux);
				}
			}
			extraerDatos(elemInfoGeneral, grupo);

			for (Element elem : entradas) {

				/*
				 * Extraer Lista de integrantes activos dentro del grupo de investigacion
				 */

				if (elem.text().startsWith("Integrantes del grupo")) {
					ArrayList<String> elemIntegrantes = new ArrayList<>();
					elemIntegrantes.add(elem.toString());
					elemIntegrantes = limpiarIntegrantes(elemIntegrantes);
					extraerIntegrantes(elemIntegrantes, grupo);
				}

				/*
				 * Extraer Producciones bibliograficas
				 */

				if (elem.text().startsWith("Artículos publicados")) {
					ArrayList<String> elemArticulos = new ArrayList<>();
					elemArticulos.add(elem.toString());
					elemArticulos = limpiar(elemArticulos);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerArticulos(elemArticulos, grupo));
					} else {
						auxProdBibliografica.addAll(extraerArticulos(elemArticulos, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Libros")) {
					ArrayList<String> elemLibros = new ArrayList<>();
					elemLibros.add(elem.toString());
					elemLibros = limpiar(elemLibros);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerLibros(elemLibros, grupo));
					} else {
						auxProdBibliografica.addAll(extraerLibros(elemLibros, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Capítulos de libro publicados")) {
					ArrayList<String> elemCapLibros = new ArrayList<>();
					elemCapLibros.add(elem.toString());
					elemCapLibros = limpiar(elemCapLibros);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerCapLibros(elemCapLibros, grupo));
					} else {
						auxProdBibliografica.addAll(extraerCapLibros(elemCapLibros, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Documentos de trabajo")) {
					ArrayList<String> elemDocumentosTrabajo = new ArrayList<>();
					elemDocumentosTrabajo.add(elem.toString());
					elemDocumentosTrabajo = limpiar(elemDocumentosTrabajo);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerDocumentosTrabajo(elemDocumentosTrabajo, grupo));
					} else {
						auxProdBibliografica.addAll(extraerDocumentosTrabajo(elemDocumentosTrabajo, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Otra publicación divulgativa")) {
					ArrayList<String> elemOtraProdBibliografica = new ArrayList<>();
					elemOtraProdBibliografica.add(elem.toString());
					elemOtraProdBibliografica = limpiar(elemOtraProdBibliografica);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(
								extraerOtraProdBibliografica(elemOtraProdBibliografica, grupo));
					} else {
						auxProdBibliografica.addAll(extraerOtraProdBibliografica(elemOtraProdBibliografica, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Otros artículos publicados")) {
					ArrayList<String> elemOtroArticulo = new ArrayList<>();
					elemOtroArticulo.add(elem.toString());
					elemOtroArticulo = limpiar(elemOtroArticulo);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerOtroArticulo(elemOtroArticulo, grupo));
					} else {
						auxProdBibliografica.addAll(extraerOtroArticulo(elemOtroArticulo, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Otros Libros publicados")) {
					ArrayList<String> elemOtroLibro = new ArrayList<>();
					elemOtroLibro.add(elem.toString());
					elemOtroLibro = limpiar(elemOtroLibro);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerOtroLibro(elemOtroLibro, grupo));
					} else {
						auxProdBibliografica.addAll(extraerOtroLibro(elemOtroLibro, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				if (elem.text().startsWith("Traducciones")) {
					ArrayList<String> elemTraduccion = new ArrayList<>();
					elemTraduccion.add(elem.toString());
					elemTraduccion = limpiar(elemTraduccion);

					List<ProduccionBibliografica> auxProdBibliografica = grupo.getProduccionBibliografica();
					if (auxProdBibliografica == null) {
						grupo.setProduccionBibliografica(extraerTraducciones(elemTraduccion, grupo));
					} else {
						auxProdBibliografica.addAll(extraerTraducciones(elemTraduccion, grupo));
						grupo.setProduccionBibliografica(auxProdBibliografica);
					}
				}

				/*
				 * Extraer Producciones tecnicas
				 */

				if (elem.text().startsWith("Cartas, mapas o similares")) {
					ArrayList<String> elemMapas = new ArrayList<>();
					elemMapas.add(elem.toString());
					elemMapas = limpiar(elemMapas);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_CARTA_MAPA, Constantes.CARTA_MAPA, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemMapas, "INSTITUCIÓN FINANCIADORA:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemMapas, "INSTITUCIÓN FINANCIADORA:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Consultorías científico tecnológicas e Informes técnicos")) {
					ArrayList<String> elemConsultorias = new ArrayList<>();
					elemConsultorias.add(elem.toString());
					elemConsultorias = limpiar(elemConsultorias);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_CONSULTORIA, Constantes.CONSULTORIA, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemConsultorias, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemConsultorias, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Diseños industriales")) {
					ArrayList<String> elemDisenios = new ArrayList<>();
					elemDisenios.add(elem.toString());
					elemDisenios = limpiar(elemDisenios);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_DISEÑO_INDUSTRIAL, Constantes.DISEÑO_INDUSTRIAL, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemDisenios, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemDisenios, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Esquemas de trazados de circuito integrado")) {
					ArrayList<String> elemEsquemas = new ArrayList<>();
					elemEsquemas.add(elem.toString());
					elemEsquemas = limpiar(elemEsquemas);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_ESQUEMA_IC, Constantes.ESQUEMA_IC, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemEsquemas, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemEsquemas, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Innovaciones en Procesos y Procedimientos")) {
					ArrayList<String> elemInnovacionesProc = new ArrayList<>();
					elemInnovacionesProc.add(elem.toString());
					elemInnovacionesProc = limpiar(elemInnovacionesProc);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_INNOVACION_PROCESO, Constantes.INNOVACION_PROCESO,
							tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemInnovacionesProc, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemInnovacionesProc, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Innovaciones generadas en la Gestión Empresarial")) {
					ArrayList<String> elemInnovacionesGest = new ArrayList<>();
					elemInnovacionesGest.add(elem.toString());
					elemInnovacionesGest = limpiar(elemInnovacionesGest);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_INNOVACION_EMPRESARIAL, Constantes.INNOVACION_EMPRESARIAL,
							tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemInnovacionesGest, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemInnovacionesGest, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Plantas piloto")) {
					ArrayList<String> elemPlantasPiloto = new ArrayList<>();
					elemPlantasPiloto.add(elem.toString());
					elemPlantasPiloto = limpiar(elemPlantasPiloto);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_PLANTA_PILOTO, Constantes.PLANTA_PILOTO, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemPlantasPiloto, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemPlantasPiloto, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Otros productos tecnológicos")) {
					ArrayList<String> elemOtrosProdTecno = new ArrayList<>();
					elemOtrosProdTecno.add(elem.toString());
					elemOtrosProdTecno = limpiar(elemOtrosProdTecno);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_OTRO_PRODUCTO_TECNOLOGICO, Constantes.OTRO_PRODUCTO_TECNOLOGICO,
							tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemOtrosProdTecno, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemOtrosProdTecno, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Prototipos")) {
					ArrayList<String> elemPrototipos = new ArrayList<>();
					elemPrototipos.add(elem.toString());
					elemPrototipos = limpiar(elemPrototipos);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_PROTOTIPO, Constantes.PROTOTIPO, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemPrototipos, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemPrototipos, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Regulaciones")) {
					ArrayList<String> elemRegulaciones = new ArrayList<>();
					elemRegulaciones.add(elem.toString());
					elemRegulaciones = limpiar(elemRegulaciones);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_NORMA, Constantes.NORMA, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemRegulaciones, "AMBITO:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemRegulaciones, "AMBITO:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Reglamentos técnicos")) {
					ArrayList<String> elemReglamentos = new ArrayList<>();
					elemReglamentos.add(elem.toString());
					elemReglamentos = limpiar(elemReglamentos);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_REGLAMENTO_TECNICO, Constantes.REGLAMENTO_TECNICO,
							tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemReglamentos, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemReglamentos, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Guias de práctica clínica")) {
					ArrayList<String> elemGuiasClinicas = new ArrayList<>();
					elemGuiasClinicas.add(elem.toString());
					elemGuiasClinicas = limpiar(elemGuiasClinicas);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_GUIA_CLINICA, Constantes.GUIA_CLINICA, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemGuiasClinicas, "AMBITO:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemGuiasClinicas, "AMBITO:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Proyectos de ley")) {
					ArrayList<String> elemProyectoLey = new ArrayList<>();
					elemProyectoLey.add(elem.toString());
					elemProyectoLey = limpiar(elemProyectoLey);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_PROYECTO_LEY, Constantes.PROYECTO_LEY, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemProyectoLey, "AMBITO:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemProyectoLey, "AMBITO:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Softwares")) {
					ArrayList<String> elemSoftwares = new ArrayList<>();
					elemSoftwares.add(elem.toString());
					elemSoftwares = limpiar(elemSoftwares);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);
					Tipo tipo = new Tipo(Constantes.ID_SOFTWARE, Constantes.SOFTWARE, tipoProduccion);

					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerProdTecnica(elemSoftwares, "DISPONIBILIDAD:", tipo, grupo));
					} else {
						auxProdTecnica.addAll(extraerProdTecnica(elemSoftwares, "DISPONIBILIDAD:", tipo, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				if (elem.text().startsWith("Empresas de base tecnológica")) {
					ArrayList<String> elemEmpresasTecno = new ArrayList<>();
					elemEmpresasTecno.add(elem.toString());
					elemEmpresasTecno = limpiar(elemEmpresasTecno);

					List<Produccion> auxProdTecnica = grupo.getProduccion();
					if (auxProdTecnica == null) {
						grupo.setProduccion(extraerEmpresasTecno(elemEmpresasTecno, grupo));
					} else {
						auxProdTecnica.addAll(extraerEmpresasTecno(elemEmpresasTecno, grupo));
						grupo.setProduccion(auxProdTecnica);
					}
				}

				/*
				 * Extraer la Apropiacion social
				 */

				if (elem.text().startsWith("Ediciones")) {
					ArrayList<String> elemEdiciones = new ArrayList<>();
					elemEdiciones.add(elem.toString());
					elemEdiciones = limpiar(elemEdiciones);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerEdiciones(elemEdiciones, grupo));
					} else {
						auxApropSocial.addAll(extraerEdiciones(elemEdiciones, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Informes de investigación")) {
					ArrayList<String> elemInformes = new ArrayList<>();
					elemInformes.add(elem.toString());
					elemInformes = limpiar(elemInformes);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerInformes(elemInformes, grupo));
					} else {
						auxApropSocial.addAll(extraerInformes(elemInformes, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Redes de Conocimiento Especializado")) {
					ArrayList<String> elemRedesConocimiento = new ArrayList<>();
					elemRedesConocimiento.add(elem.toString());
					elemRedesConocimiento = limpiar(elemRedesConocimiento);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerRedesYParticipacion(elemRedesConocimiento, grupo));
					} else {
						auxApropSocial.addAll(extraerRedesYParticipacion(elemRedesConocimiento, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Generación de Contenido Impreso")) {
					ArrayList<String> elemContImpreso = new ArrayList<>();
					elemContImpreso.add(elem.toString());
					elemContImpreso = limpiar(elemContImpreso);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerContenidoImpresoMultimediaVirtual(elemContImpreso, grupo));
					} else {
						auxApropSocial.addAll(extraerContenidoImpresoMultimediaVirtual(elemContImpreso, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Generación de Contenido Multimedia")) {
					ArrayList<String> elemContMultimedia = new ArrayList<>();
					elemContMultimedia.add(elem.toString());
					elemContMultimedia = limpiar(elemContMultimedia);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerContenidoImpresoMultimediaVirtual(elemContMultimedia, grupo));
					} else {
						auxApropSocial.addAll(extraerContenidoImpresoMultimediaVirtual(elemContMultimedia, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Generación de Contenido Virtual")) {
					ArrayList<String> elemContVirtual = new ArrayList<>();
					elemContVirtual.add(elem.toString());
					elemContVirtual = limpiar(elemContVirtual);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerContenidoImpresoMultimediaVirtual(elemContVirtual, grupo));
					} else {
						auxApropSocial.addAll(extraerContenidoImpresoMultimediaVirtual(elemContVirtual, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Estrategias de Comunicación del Conocimiento")) {
					ArrayList<String> elemEstComunicacion = new ArrayList<>();
					elemEstComunicacion.add(elem.toString());
					elemEstComunicacion = limpiar(elemEstComunicacion);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerEstrategiasComunicacion(elemEstComunicacion, grupo));
					} else {
						auxApropSocial.addAll(extraerEstrategiasComunicacion(elemEstComunicacion, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Estrategias Pedagógicas para el fomento a la CTI")) {
					ArrayList<String> elemEstPedagogicasCTI = new ArrayList<>();
					elemEstPedagogicasCTI.add(elem.toString());
					elemEstPedagogicasCTI = limpiar(elemEstPedagogicasCTI);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerEstrategiasYParticipacionCti(elemEstPedagogicasCTI, grupo));
					} else {
						auxApropSocial.addAll(extraerEstrategiasYParticipacionCti(elemEstPedagogicasCTI, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Espacios de Participación Ciudadana")) {
					ArrayList<String> elemEspPartCiudadana = new ArrayList<>();
					elemEspPartCiudadana.add(elem.toString());
					elemEspPartCiudadana = limpiar(elemEspPartCiudadana);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerRedesYParticipacion(elemEspPartCiudadana, grupo));
					} else {
						auxApropSocial.addAll(extraerRedesYParticipacion(elemEspPartCiudadana, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Participación Ciudadana en Proyectos de CTI")) {
					ArrayList<String> elemPartCiudCTI = new ArrayList<>();
					elemPartCiudCTI.add(elem.toString());
					elemPartCiudCTI = limpiar(elemPartCiudCTI);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerEstrategiasYParticipacionCti(elemPartCiudCTI, grupo));
					} else {
						auxApropSocial.addAll(extraerEstrategiasYParticipacionCti(elemPartCiudCTI, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				if (elem.text().startsWith("Eventos Científicos")) {
					ArrayList<String> elemEventos = new ArrayList<>();
					elemEventos.add(elem.toString());
					elemEventos = limpiar(elemEventos);

					List<Produccion> auxApropSocial = grupo.getProduccion();
					if (auxApropSocial == null) {
						grupo.setProduccion(extraerEventos(elemEventos, grupo));
					} else {
						auxApropSocial.addAll(extraerEventos(elemEventos, grupo));
						grupo.setProduccion(auxApropSocial);
					}
				}

				/*
				 * Extraer las Actividades de Formacion
				 */

				if (elem.text().contains("Curso de Corta Duración Dictados")) {
					ArrayList<String> elemCursosCortaDuracion = new ArrayList<>();
					elemCursosCortaDuracion.add(elem.toString());
					elemCursosCortaDuracion = limpiar(elemCursosCortaDuracion);

					List<Produccion> auxFormacion = grupo.getProduccion();
					if (auxFormacion == null) {
						grupo.setProduccion(extraerCursosCortos(elemCursosCortaDuracion, "IDIOMA:", grupo));
					} else {
						auxFormacion.addAll(extraerCursosCortos(elemCursosCortaDuracion, "IDIOMA:", grupo));
						grupo.setProduccion(auxFormacion);
					}
				}

				if (elem.text().contains("Trabajos dirigidos/turorías")) {
					ArrayList<String> elemTrabajosDirigidos = new ArrayList<>();
					elemTrabajosDirigidos.add(elem.toString());
					elemTrabajosDirigidos = limpiar(elemTrabajosDirigidos);

					List<Produccion> auxFormacion = grupo.getProduccion();
					if (auxFormacion == null) {
						grupo.setProduccion(extraerTrabajosDirigidos(elemTrabajosDirigidos, grupo));
					} else {
						auxFormacion.addAll(extraerTrabajosDirigidos(elemTrabajosDirigidos, grupo));
						grupo.setProduccion(auxFormacion);
					}
				}

				/*
				 * Extraer las Actividades de Evaluacion
				 */

				if (elem.text().contains("Jurado/Comisiones evaluadoras de trabajo de grado")) {
					ArrayList<String> elemJuradoComite = new ArrayList<>();
					elemJuradoComite.add(elem.toString());
					elemJuradoComite = limpiar(elemJuradoComite);

					List<Produccion> auxEvaluacion = grupo.getProduccion();
					if (auxEvaluacion == null) {
						grupo.setProduccion(extraerJurado(elemJuradoComite, grupo));
					} else {
							auxEvaluacion.addAll(extraerJurado(elemJuradoComite, grupo));
							grupo.setProduccion(auxEvaluacion);						
					}
				}

				if (elem.text().contains("Participación en comités de evaluación")) {
					ArrayList<String> elemPartComites = new ArrayList<>();
					elemPartComites.add(elem.toString());
					elemPartComites = limpiar(elemPartComites);

					List<Produccion> auxEvaluacion = grupo.getProduccion();
					if (auxEvaluacion == null) {
						grupo.setProduccion(extraerPartipacionComites(elemPartComites, grupo));
					} else {
						auxEvaluacion.addAll(extraerPartipacionComites(elemPartComites, grupo));
						grupo.setProduccion(auxEvaluacion);
					}
				}

				/*
				 * Extraer Informacion adicional
				 */

				if (elem.text().contains("Demás trabajos")) {
					ArrayList<String> elemDemasTrabajos = new ArrayList<>();
					elemDemasTrabajos.add(elem.toString());
					elemDemasTrabajos = limpiar(elemDemasTrabajos);

					List<Produccion> auxProduccion = grupo.getProduccion();
					if (auxProduccion == null) {
						grupo.setProduccion(extraerDemasTrabajos(elemDemasTrabajos, grupo));
					} else {
						auxProduccion.addAll(extraerDemasTrabajos(elemDemasTrabajos, grupo));
						grupo.setProduccion(auxProduccion);
					}
				}

				if (elem.text().startsWith("Proyectos 1")) {
					ArrayList<String> elemProyectos = new ArrayList<>();
					elemProyectos.add(elem.toString());
					elemProyectos = limpiar(elemProyectos);

					List<Produccion> auxProduccion = grupo.getProduccion();
					if (auxProduccion == null) {
						grupo.setProduccion(extraerProyectos(elemProyectos, grupo));
					} else {
						auxProduccion.addAll(extraerProyectos(elemProyectos, grupo));
						grupo.setProduccion(auxProduccion);
					}
				}

				/*
				 * Extraer Producciones en Arte
				 */

				if (elem.text().startsWith("Obras o productos")) {
					ArrayList<String> elemObras = new ArrayList<>();
					elemObras.add(elem.toString());
					elemObras = limpiar(elemObras);

					List<Produccion> auxProdArte = grupo.getProduccion();
					if (auxProdArte == null) {
						grupo.setProduccion(extraerObras(elemObras, grupo));
					} else {
						auxProdArte.addAll(extraerObras(elemObras, grupo));
						grupo.setProduccion(auxProdArte);
					}
				}

				if (elem.text().startsWith("Registros de acuerdo de licencia")) {
					ArrayList<String> elemRegistrosdeAcuerdo = new ArrayList<>();
					elemRegistrosdeAcuerdo.add(elem.toString());
					elemRegistrosdeAcuerdo = limpiar(elemRegistrosdeAcuerdo);

					List<Produccion> auxProdArte = grupo.getProduccion();
					if (auxProdArte == null) {
						grupo.setProduccion(extraerRegistrosAcuerdo(elemRegistrosdeAcuerdo, grupo));
					} else {
						auxProdArte.addAll(extraerRegistrosAcuerdo(elemRegistrosdeAcuerdo, grupo));
						grupo.setProduccion(auxProdArte);
					}
				}

				if (elem.text().startsWith("Industrias Creativas y culturales")) {
					ArrayList<String> elemIndustriasCreativas = new ArrayList<>();
					elemIndustriasCreativas.add(elem.toString());
					elemIndustriasCreativas = limpiar(elemIndustriasCreativas);

					List<Produccion> auxProdArte = grupo.getProduccion();
					if (auxProdArte == null) {
						grupo.setProduccion(extraerIndustrias(elemIndustriasCreativas, grupo));
					} else {
						auxProdArte.addAll(extraerIndustrias(elemIndustriasCreativas, grupo));
						grupo.setProduccion(auxProdArte);
					}
				}

				if (elem.text().startsWith("Eventos artísticos")) {
					ArrayList<String> elemEventoArtistico = new ArrayList<>();
					elemEventoArtistico.add(elem.toString());
					elemEventoArtistico = limpiar(elemEventoArtistico);

					List<Produccion> auxProdArte = grupo.getProduccion();
					if (auxProdArte == null) {
						grupo.setProduccion(extraerEventoArtistico(elemEventoArtistico, grupo));
					} else {
						auxProdArte.addAll(extraerEventoArtistico(elemEventoArtistico, grupo));
						grupo.setProduccion(auxProdArte);
					}
				}

				if (elem.text().startsWith("Talleres Creativos")) {
					ArrayList<String> elemTallerCreativo = new ArrayList<>();
					elemTallerCreativo.add(elem.toString());
					elemTallerCreativo = limpiar(elemTallerCreativo);

					List<Produccion> auxProdArte = grupo.getProduccion();
					if (auxProdArte == null) {
						grupo.setProduccion(extraerTallerCreativo(elemTallerCreativo, grupo));
					} else {
						auxProdArte.addAll(extraerTallerCreativo(elemTallerCreativo, grupo));
						grupo.setProduccion(auxProdArte);
					}
				}
			}

			grupos.add(grupo);
			System.out.println(grupo.getNombre());

		} else {
			System.out.println("El Status Code no es OK es: " + getStatusConnectionCode(url));
		}

	}

	/**
	 * Metodo que elimina las etiquetas y caracteres especiales en la lista que
	 * tiene la estructura de la pagina web del cvlac de cada investigador
	 * 
	 * @param elementos,
	 *            lista que contiene la estructura textual de la pagina web
	 * @return Lista con la estructura de la pagina web sin las etiquetas y los
	 *         caracteres especiales
	 */
	public ArrayList<String> limpiar(ArrayList<String> elementos) {
		String temporal = "";
		ArrayList<String> elementosLimpio = new ArrayList<>();
		ArrayList<String> aux = new ArrayList<>();
		ArrayList<String> aux2 = new ArrayList<>();
		for (int i = 0; i < elementos.size(); i++) {
			temporal = elementos.get(i).replaceAll("\n", "");
			temporal = temporal.replaceAll("&nbsp;", " ");
			temporal = temporal.replaceAll("  ", " ");
			temporal = temporal.replaceAll("&AMP", "&");
			temporal = temporal.replaceAll("&AMP;", "&");
			temporal = temporal.replaceAll("'", "");
		}
		char[] auxiliar = temporal.toCharArray();
		int posI = 0;
		int posF = 0;
		for (int j = 0; j < auxiliar.length; j++) {
			if ((auxiliar[j] == 'h') && (auxiliar[j + 1] == 'r') && (auxiliar[j + 2] == 'e')) {

				boolean primeraComilla = false;
				boolean segundaComilla = false;

				for (int i = j; i < auxiliar.length; i++) {
					if (auxiliar[i] == '"' && !primeraComilla) {
						auxiliar[i] = '>';
						primeraComilla = true;
						i++;
					}
					if (auxiliar[i] == '"' && !segundaComilla) {
						auxiliar[i] = '<';
						segundaComilla = true;
						break;
					}
				}
			}
			if (auxiliar[j] == '>') {
				posI = j;
				for (int i = j; i < auxiliar.length; i++) {
					if (auxiliar[i] == '<') {
						posF = i;
						elementosLimpio.add(temporal.substring(posI, posF));
						j = i;
						break;
					}
				}
			}
		}
		for (int i = 0; i < elementosLimpio.size(); i++) {
			temporal = "";
			temporal = elementosLimpio.get(i).replaceAll(">", " ");
			aux.add(temporal);

		}
		for (int i = 0; i < aux.size(); i++) {
			if (!aux.get(i).equals(" ")) {
				temporal = "";
				temporal = aux.get(i).substring(1);
				aux2.add(temporal.trim());
			}

		}
		aux.clear();

		for (int i = 0; i < aux2.size(); i++) {
			temporal = aux2.get(i);
			if (!temporal.equals("")) {
				aux.add(temporal.trim().toUpperCase());
			}

		}

		elementosLimpio = aux;
		return elementosLimpio;
	}

	/**
	 * Metodo que elimina las etiquetas y caracteres especiales en la lista que
	 * tiene la estructura de la pagina web del cvlac de cada investigador
	 * 
	 * @param elem,
	 *            lista que contiene la estructura textual de la pagina web
	 * @return Lista con la estructura de la pagina web sin las etiquetas y los
	 *         caracteres especiales
	 */
	public ArrayList<String> limpiarIntegrantes(ArrayList<String> elem) {
		String temporal = "";
		ArrayList<String> elementosLimpio = new ArrayList<>();
		ArrayList<String> aux = new ArrayList<>();
		ArrayList<String> aux2 = new ArrayList<>();
		for (int i = 0; i < elem.size(); i++) {
			temporal = elem.get(i).replaceAll("\n", "");
			temporal = temporal.replaceAll("&nbsp;", " ");
			temporal = temporal.replaceAll("  ", " ");
			temporal = temporal.replaceAll("&AMP", "&");
			temporal = temporal.replaceAll("&AMP;", "&");
			temporal = temporal.replaceAll("'", "");
		}
		char[] auxiliar = temporal.toCharArray();
		int posI = 0;
		int posF = 0;
		for (int j = 0; j < auxiliar.length; j++) {
			if ((auxiliar[j] == 'h') && (auxiliar[j + 1] == 'r') && (auxiliar[j + 2] == 'e')) {

				boolean primeraComilla = false;
				boolean segundaComilla = false;

				for (int i = j; i < auxiliar.length; i++) {
					if (auxiliar[i] == '"' && !primeraComilla) {
						auxiliar[i] = '>';
						primeraComilla = true;
						i++;
					}
					if (auxiliar[i] == '"' && !segundaComilla) {
						auxiliar[i] = '<';
						segundaComilla = true;
						break;
					}
				}
			}
			if (auxiliar[j] == '>') {
				posI = j;
				for (int i = j; i < auxiliar.length; i++) {
					if (auxiliar[i] == '<') {
						posF = i;
						elementosLimpio.add(temporal.substring(posI, posF));
						j = i;
						break;
					}
				}
			}
		}
		for (int i = 0; i < elementosLimpio.size(); i++) {
			temporal = "";
			temporal = elementosLimpio.get(i).replaceAll(">", " ");
			aux.add(temporal);

		}
		for (int i = 0; i < aux.size(); i++) {
			if (!aux.get(i).equals(" ")) {
				temporal = "";
				temporal = aux.get(i).substring(1);
				aux2.add(temporal.trim());
			}

		}
		aux.clear();

		for (int i = 0; i < aux2.size(); i++) {
			temporal = aux2.get(i);
			if (!temporal.equals("")) {
				aux.add(temporal.trim());
			}

		}

		elementosLimpio = aux;
		return elementosLimpio;
	}

	/*
	 * Extraer Datos Generales del grupo
	 */

	/**
	 * 
	 * @param elemInfoGeneral
	 * @param id
	 * @return
	 */
	public void extraerDatos(ArrayList<String> elemInfoGeneral, Grupo grupo) {

		try {

			ArrayList<LineasInvestigacion> lineas = new ArrayList<>();
			String nomLinea = "";

			grupo.setNombre(nombreGrupo);

			for (int i = 0; i < elemInfoGeneral.size(); i++) {

				// Extraccion del año en que se formo el grupo de investigacion

				if (elemInfoGeneral.get(i).startsWith("AÑO Y MES DE FORMACIÓN")) {
					String anioFormacion = elemInfoGeneral.get(i + 1);
					grupo.setAnioFundacion(anioFormacion);
				}

				// Extraccion del Lider del grupo de investigacion

				if (elemInfoGeneral.get(i).startsWith("LÍDER")) {
					grupo.setLider(elemInfoGeneral.get(i + 1));
				}

				// Extraccion de la categoria del grupo de investigacion

				if (elemInfoGeneral.get(i).startsWith("CLASIFICACIÓN")) {
					grupo.setCategoria(elemInfoGeneral.get(i + 1));
					if (grupo.getCategoria().equalsIgnoreCase("ÁREA DE CONOCIMIENTO")) {
						grupo.setCategoria("SIN CATEGORÍA");
					}
				}

				// Extraccion del area de conocimiento que cobija al grupo de investigacion

				if (elemInfoGeneral.get(i).startsWith("ÁREA DE CONOCIMIENTO")) {
					grupo.setAreaConocimiento(elemInfoGeneral.get(i + 1));
				}

				// Extraccion de las lineas de investigacion

				try {
					if (elemInfoGeneral.get(i).contains(".-")) {
						nomLinea = elemInfoGeneral.get(i).substring(elemInfoGeneral.get(i).indexOf(".- ") + 3);
						LineasInvestigacion lineasInvestigacion = new LineasInvestigacion();
						nomLinea= StringUtils.stripAccents(nomLinea);
						nomLinea= nomLinea.trim();
						lineasInvestigacion.setNombre(nomLinea);
						lineas.add(lineasInvestigacion);
					}

					grupo.setLineasInvestigacion(lineas);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo que extrae la lista de investigadores y sus respectivos links
	 * 
	 * @param elem
	 * @param grupo
	 */
	public void extraerIntegrantes(ArrayList<String> elem, Grupo grupo) {

		String link = "";

		for (int i = 0; i < elem.size(); i++) {

			if (elem.get(i).contains(".-")) {
				link = elem.get(i + 1);
				if (elem.get(i + 3).contains("Actual")) {
					CvLac cvLac = new CvLac();
					Investigador auxInvestigador = cvLac.extraer(link, "ACTUAL");
					GruposInves gruposInves = new GruposInves(grupo, auxInvestigador, "ACTUAL");
					grupoInves.add(gruposInves);
				} else if (elem.get(i + 4).contains("Actual")) {
					CvLac cvLac = new CvLac();
					Investigador auxInvestigador = cvLac.extraer(link, "ACTUAL");
					GruposInves gruposInves = new GruposInves(grupo, auxInvestigador, "ACTUAL");
					grupoInves.add(gruposInves);
				} else if (elem.get(i + 5).contains("Actual")) {
					CvLac cvLac = new CvLac();
					Investigador auxInvestigador = cvLac.extraer(link, "ACTUAL");
					GruposInves gruposInves = new GruposInves(grupo, auxInvestigador, "ACTUAL");
					grupoInves.add(gruposInves);
				} else {
					CvLac cvLac = new CvLac();
					Investigador auxInvestigador = cvLac.extraer(link, "NO ACTUAL");
					GruposInves gruposInves = new GruposInves(grupo, auxInvestigador, "NO ACTUAL");
					grupoInves.add(gruposInves);
				}
			}
		}
	}

	/*
	 * Extraer Producciones bibliograficas
	 */

	/***
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerArticulos(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String issn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {
				if (elem.get(i + 1).contains("PUBLICADO EN REVISTA ESPECIALIZADA:")) {

					tipo = new Tipo(Constantes.ID_ARTICULO, Constantes.ARTICULO, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_OTRO_ARTICULO, Constantes.OTRO_ARTICULO, tipoProduccion);

				}

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.endsWith("DOI:")) {
					referencia = referencia.substring(1, referencia.length() - 6);
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("ISSN:")) {

				String aux = elem.get(i).trim();
				aux = aux.substring(aux.indexOf("ISSN"), aux.indexOf("VOL:"));
				issn = aux.substring(aux.indexOf(": ") + 2, aux.indexOf(","));
				anio = aux.substring(aux.indexOf(",") + 2, aux.indexOf(",") + 6);

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				referencia = referencia.trim();
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(issn);
				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerLibros(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String isbn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {
				if (elem.get(i + 1).contains("LIBRO RESULTADO DE INVESTIGACIÓN")) {

					tipo = new Tipo(Constantes.ID_LIBRO, Constantes.LIBRO, tipoProduccion);

				}

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("ISBN")) {
				char[] aux = elem.get(i).toCharArray();
				for (int j = 0; j < aux.length; j++) {
					if (aux[j] == 'I' && aux[j + 1] == 'S' && aux[j + 2] == 'B') {
						int posI = j + 6;
						int posF = posI;
						for (int k = posI; k < aux.length; k++) {
							try {
								if (aux[k + 1] == 'V' && aux[k + 2] == 'O' && aux[k + 3] == 'L' && aux[k + 4] == ':') {
									posF = k;
									break;
								}
							} catch (Exception e) {
								posF = posI;
							}

						}
						isbn = elem.get(i).substring(posI, posF);
						int posAux = elem.get(i).indexOf(',') + 1;
						anio = elem.get(i).substring(posAux, posAux + 4);
						break;
					}
				}

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(isbn);
				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * Metodo que extrae los capitulos de libro que publico el grupo de
	 * investigacion
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los capitulos de libro
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerCapLibros(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String isbn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {
				if (!elem.get(i).contains("OTRO CAPÍTULO")) {

					tipo = new Tipo(Constantes.ID_CAPITULO_LIBRO, Constantes.CAPITULO_LIBRO, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_OTRO_CAPITULO_LIBRO, Constantes.OTRO_CAPITULO_LIBRO, tipoProduccion);

				}

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("ISBN")) {
				char[] aux = elem.get(i).toCharArray();
				for (int j = 0; j < aux.length; j++) {
					if (aux[j] == 'I' && aux[j + 1] == 'S' && aux[j + 2] == 'B') {
						int posI = j + 6;
						int posF = posI;
						for (int k = posI; k < aux.length; k++) {
							try {
								if (aux[k + 2] == 'V' && aux[k + 3] == 'O' && aux[k + 4] == 'L' && aux[k + 5] == '.') {
									posF = k;
									break;
								}
							} catch (Exception e) {
								posF = posI;
							}

						}
						isbn = elem.get(i).substring(posI, posF);
						int posAux = elem.get(i).indexOf(',') + 2;
						anio = elem.get(i).substring(posAux, posAux + 4);
						break;
					}
				}

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(isbn);
				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerDocumentosTrabajo(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String id = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {
				if (elem.get(i + 1).contains("DOCUMENTO DE TRABAJO")) {
					id = "DOCUMENTO DE TRABAJO";

					tipo = new Tipo(Constantes.ID_DOCUMENTO_TRABAJO, Constantes.DOCUMENTO_TRABAJO, tipoProduccion);

				}

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.endsWith("DOI:")) {
					referencia = referencia.substring(1, referencia.length() - 6);
				}

				if (referencia.startsWith(": ")) {
					referencia = referencia.substring(2);
				}

			}

			if (elem.get(i).contains("PAGINAS:")) {

				int posAux = elem.get(i).indexOf(',');
				anio = elem.get(i).substring(0, posAux);

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(id);
				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerOtraProdBibliografica(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String id = "OTRA PUBLICACIÓN DIVULGATIVA";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("VOL.")) {

				int posAux = elem.get(i).indexOf(',') + 2;
				anio = elem.get(i).substring(posAux, posAux + 4);

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(id);
				produccionBibliografica.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_OTRA_PUBLICACION_DIVULGATIVA,
						Constantes.OTRA_PUBLICACION_DIVULGATIVA, tipoProduccion);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerOtroArticulo(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String issn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("ISSN:")) {

				String aux = elem.get(i).substring(elem.get(i).indexOf("ISSN: "));
				int posAux = aux.indexOf(':') + 2;
				issn = aux.substring(posAux, aux.indexOf(","));
				anio = aux.substring(aux.indexOf(",") + 2, aux.indexOf(",") + 6);

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(issn);
				produccionBibliografica.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_OTRO_ARTICULO, Constantes.OTRO_ARTICULO, tipoProduccion);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerOtroLibro(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String isbn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {

				referencia = "";
				for (int j = i + 2; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("ISBN:")) {

				int posAux = elem.get(i).indexOf(',');
				anio = elem.get(i).substring(posAux + 1, posAux + 5);
				try {
					isbn = elem.get(i).substring(elem.get(i).indexOf("ISBN:") + 6, elem.get(i).indexOf("VOL:") - 1);

				} catch (Exception e) {
					isbn = "";
				}

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(isbn);
				produccionBibliografica.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_OTRO_LIBRO, Constantes.OTRO_LIBRO, tipoProduccion);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<ProduccionBibliografica> extraerTraducciones(ArrayList<String> elem, Grupo grupo) {

		String autores = "";
		String referencia = "";
		String anio = "";
		String id = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

			if (elem.get(i).contains(".-")) {

				referencia = "";
				for (int j = i + 1; j < elem.size(); j++) {
					if (!elem.get(j).contains("AUTORES:")) {
						referencia += " " + elem.get(j);
					} else {
						break;
					}
				}

				if (referencia.startsWith(" : ")) {
					referencia = referencia.substring(3);
				}

			}

			if (elem.get(i).contains("LIBRO: ISBN")) {
				anio = elem.get(i).substring(0, elem.get(i).indexOf(','));
				String aux = elem.get(i).substring(elem.get(i).indexOf("ISSN"));
				int posAux = aux.indexOf(',');
				id = aux.substring(4, posAux);

			}

			if (elem.get(i).contains("REVISTA: ISSN")) {
				anio = elem.get(i).substring(0, elem.get(i).indexOf(','));
				String aux = elem.get(i).substring(elem.get(i).indexOf("ISBN "));
				int posAux = aux.indexOf(',');
				id = aux.substring(4, posAux);

			}

			if (elem.get(i).contains("AUTORES:")) {
				autores = elem.get(i).substring(9, elem.get(i).length() - 1);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(id);
				produccionBibliografica.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_TRADUCCION, Constantes.TRADUCCION, tipoProduccion);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setGrupo(grupo);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return prodBibliograficaAux;
	}

	/*
	 * Extraer Producciones tecnicas
	 */

	/**
	 * 
	 * @param elem
	 * @param bandera
	 * @param tipo
	 * @return
	 */
	public ArrayList<Produccion> extraerProdTecnica(ArrayList<String> elem, String bandera, Tipo tipo, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		ArrayList<Produccion> prodTecnicaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionTecnica = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += ", " + actual;

					if (actual.contains(bandera)) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos + 2, pos + 6);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(4, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccionTecnica.setAnio(anio);
				produccionTecnica.setAutores(autores);
				produccionTecnica.setReferencia(referencia);
				produccionTecnica.setTipo(tipo);
				produccionTecnica.setGrupo(grupo);
				produccionTecnica.setRepetido("NO");
				identificarRepetidos(prodTecnicaAux, produccionTecnica);
				prodTecnicaAux.add(produccionTecnica);
			}
		}

		return prodTecnicaAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEmpresasTecno(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodTecnicaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionTecnica = new Produccion();
			if (elem.get(i).contains(".-")) {

				tipo = new Tipo(Constantes.ID_EMPRESA_TECNOLOGICA, Constantes.EMPRESA_TECNOLOGICA, tipoProduccion);

				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += ", " + actual;

					if (actual.contains("NIT:")) {
						int pos = actual.indexOf(",");
						if (pos >= 3) {
							anio = actual.substring(pos - 4, pos);
						}
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(4, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccionTecnica.setAnio(anio);
				produccionTecnica.setAutores(autores);
				produccionTecnica.setReferencia(referencia);
				produccionTecnica.setTipo(tipo);
				produccionTecnica.setGrupo(grupo);
				produccionTecnica.setRepetido("NO");
				identificarRepetidos(prodTecnicaAux, produccionTecnica);
				prodTecnicaAux.add(produccionTecnica);

			}
		}

		return prodTecnicaAux;
	}

	/*
	 * Extraer la Apropiacion social
	 */

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEdiciones(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("EDITORIAL:")) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos + 2, pos + 6);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(3, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_EDICION, Constantes.EDICION, tipoProduccion);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerInformes(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("PROYECTO DE INVESTIGACIÓN:")) {
						int pos = actual.indexOf(",");
						anio = actual.substring(0, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(3, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_INFORME_INVESTIGACION, Constantes.INFORME_INVESTIGACION,
						tipoProduccion);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerRedesYParticipacion(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains("REDES DE CONOCIMIENTO")) {

				tipo = new Tipo(Constantes.ID_RED, Constantes.RED, tipoProduccion);

			} else if (elem.get(i).contains("ESPACIOS DE PARTICIPACIÓN CIUDADANA")) {

				tipo = new Tipo(Constantes.ID_ESPACIO_PARTICIPACION, Constantes.ESPACIO_PARTICIPACION, tipoProduccion);

			}
			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("DESDE")) {
						int pos = actual.indexOf("-");
						anio = actual.substring(pos - 4, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerContenidoImpresoMultimediaVirtual(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains("GENERACIÓN DE CONTENIDO IMPRESO")) {

				tipo = new Tipo(Constantes.ID_CONTENIDO_IMPRESO, Constantes.CONTENIDO_IMPRESO, tipoProduccion);

			} else if (elem.get(i).contains("GENERACIÓN DE CONTENIDO MULTIMEDIA")) {

				tipo = new Tipo(Constantes.ID_CONTENIDO_MULTIMEDIA, Constantes.CONTENIDO_MULTIMEDIA, tipoProduccion);

			} else if (elem.get(i).contains("GENERACIÓN DE CONTENIDO VIRTUAL")) {

				tipo = new Tipo(Constantes.ID_CONTENIDO_VIRTUAL, Constantes.CONTENIDO_VIRTUAL, tipoProduccion);

			}
			if (elem.get(i).contains(".-")) {
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("AMBITO:")) {
						int pos = actual.indexOf("-");
						try {
							anio = actual.substring(0, pos);
						} catch (Exception e) {
							anio = "N/D";
						}

					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(3, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEstrategiasComunicacion(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains(": DESDE")) {
						int pos = actual.length();
						anio = actual.substring(pos - 4, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_ESTRATEGIA_COMUNICACION, Constantes.ESTRATEGIA_COMUNICACION,
						tipoProduccion);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEstrategiasYParticipacionCti(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")
						&& !elem.get(cont).contains("DESCRIPCIÓN:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains(": DESDE")) {
						int pos = actual.length();
						anio = actual.substring(pos - 4, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_ESTRATEGIA_PEDAGOGICA, Constantes.ESTRATEGIA_PEDAGOGICA,
						tipoProduccion);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEventos(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> auxProduccion = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion apropiacionSocial = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")
						&& !elem.get(cont).contains("INSTITUCIONES ASOCIADAS")
						&& !elem.get(cont).contains("NOMBRE DE LA INSTITUCIÓN:")
						&& !elem.get(cont).contains("TIPO DE VINCULACIÓN")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("- HASTA")) {
						int pos = actual.indexOf("-");
						anio = actual.substring(pos - 4, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				apropiacionSocial.setAnio(anio);
				apropiacionSocial.setAutores(autores);
				apropiacionSocial.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_EVENTO_CINTIFICO, Constantes.EVENTO_CIENTIFICO, tipoProduccion);
				apropiacionSocial.setTipo(tipo);
				apropiacionSocial.setGrupo(grupo);
				apropiacionSocial.setRepetido("NO");
				identificarRepetidos(auxProduccion, apropiacionSocial);
				auxProduccion.add(apropiacionSocial);
			}
		}

		return auxProduccion;
	}

	/*
	 * Extraer Actividades de Formacion
	 */

	/**
	 * 
	 * @param elem
	 * @param bandera
	 * @return
	 */
	public ArrayList<Produccion> extraerCursosCortos(ArrayList<String> elem, String bandera, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_FORMACION, Constantes.FORMACION);

		ArrayList<Produccion> actFormacionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion actividadesFormacion = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += ", " + actual;

					if (actual.contains(bandera)) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos + 2, pos + 6);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(4, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				actividadesFormacion.setAnio(anio);
				actividadesFormacion.setAutores(autores);
				actividadesFormacion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_CURSO_CORTO, Constantes.CURSO_CORTO, tipoProduccion);
				actividadesFormacion.setTipo(tipo);
				actividadesFormacion.setGrupo(grupo);
				actividadesFormacion.setRepetido("NO");
				identificarRepetidos(actFormacionAux, actividadesFormacion);
				actFormacionAux.add(actividadesFormacion);
			}
		}

		return actFormacionAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerTrabajosDirigidos(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_FORMACION, Constantes.FORMACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> actFormacionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion actividadesFormacion = new Produccion();

			if (elem.get(i).contains(".-")) {
				if (elem.get(i + 1).contains("PREGRADO")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_P, Constantes.TRABAJO_GRADO_P, tipoProduccion);

				} else if (elem.get(i + 1).contains("MAESTRÍA")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_M, Constantes.TRABAJO_GRADO_M, tipoProduccion);

				} else if (elem.get(i + 1).contains("DOCTORADO")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_D, Constantes.TRABAJO_GRADO_D, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_TUTORIA, Constantes.TUTORIA, tipoProduccion);

				}
				int cont = i + 2;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += ", " + actual;

					if (actual.contains("TIPO DE ORIENTACIÓN:")) {
						int pos = actual.indexOf("HASTA");
						if (pos >= 3) {
							anio = actual.substring(pos - 5, pos - 1);
						}
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.substring(4, referencia.length() - 1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				actividadesFormacion.setAnio(anio);
				actividadesFormacion.setAutores(autores);
				actividadesFormacion.setReferencia(referencia);
				actividadesFormacion.setTipo(tipo);
				actividadesFormacion.setGrupo(grupo);
				actividadesFormacion.setRepetido("NO");
				identificarRepetidos(actFormacionAux, actividadesFormacion);
				actFormacionAux.add(actividadesFormacion);
			}
		}

		return actFormacionAux;
	}

	/*
	 * Extraer las Actividades de Evaluacion
	 */

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerJurado(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_EVALUADOR, Constantes.EVALUADOR);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> actEvaluadorAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion actividadesEvaluador = new Produccion();

			if (elem.get(i).contains("JURADO/COMISIONES EVALUADORAS DE TRABAJO DE GRADO")) {

				tipo = new Tipo(Constantes.ID_JURADO_COMITE_EVALUADOR, Constantes.JURADO_COMITE_EVALUADOR,
						tipoProduccion);

			}
			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("IDIOMA:")) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos + 2, pos + 6);
					} else if (actual.contains("AUTORES:")&& actual.length()>8) {
						autores = actual.substring(9, actual.length() - 1);
					}else {
						autores = "NO ESPECIFICADO";
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				actividadesEvaluador.setAnio(anio);
				actividadesEvaluador.setAutores(autores);
				actividadesEvaluador.setReferencia(referencia);
				actividadesEvaluador.setTipo(tipo);
				actividadesEvaluador.setGrupo(grupo);
				actividadesEvaluador.setRepetido("NO");
				identificarRepetidos(actEvaluadorAux, actividadesEvaluador);
				actEvaluadorAux.add(actividadesEvaluador);
			}
		}

		return actEvaluadorAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerPartipacionComites(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_EVALUADOR, Constantes.EVALUADOR);

		ArrayList<Produccion> actEvaluadorAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion actividadesEvaluador = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("SITIO WEB:")) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos - 4, pos);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				actividadesEvaluador.setAnio(anio);
				actividadesEvaluador.setAutores(autores);
				actividadesEvaluador.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_PARTICIPACION_COMITE_EVALUADOR,
						Constantes.PARTICIPACION_COMITE_EVALUADOR, tipoProduccion);
				actividadesEvaluador.setTipo(tipo);
				actividadesEvaluador.setGrupo(grupo);
				actividadesEvaluador.setRepetido("NO");
				identificarRepetidos(actEvaluadorAux, actividadesEvaluador);
				actEvaluadorAux.add(actividadesEvaluador);
			}
		}

		return actEvaluadorAux;
	}

	/*
	 * Extraer Informacion adicional
	 */

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerDemasTrabajos(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_MASINFORMACION, Constantes.MASINFORMACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> masInformacionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion masInformacion = new Produccion();

			if (elem.get(i).contains("DEMÁS TRABAJOS")) {

				tipo = new Tipo(Constantes.ID_DEMAS_TRABAJOS, Constantes.DEMAS_TRABAJOS, tipoProduccion);
			}
			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (actual.contains("IDIOMA:")) {
						int pos = actual.indexOf(",");
						anio = actual.substring(pos + 2, pos + 6);
					} else if (actual.contains("AUTORES:")) {
						autores = actual.substring(9, actual.length() - 1);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				masInformacion.setAnio(anio);
				masInformacion.setAutores(autores);
				masInformacion.setReferencia(referencia);
				masInformacion.setTipo(tipo);
				masInformacion.setGrupo(grupo);
				masInformacion.setRepetido("NO");
				identificarRepetidos(masInformacionAux, masInformacion);
				masInformacionAux.add(masInformacion);
			}
		}

		return masInformacionAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerProyectos(ArrayList<String> elem, Grupo grupo) {
		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_MASINFORMACION, Constantes.MASINFORMACION);

		ArrayList<Produccion> masInformacionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion masInformacion = new Produccion();

			if (elem.get(i).contains(".-")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains(".-")) {
					String actual = elem.get(cont);
					referencia += " " + actual;

					if (elem.get(i + 3).contains(".-")) {
						anio = "N/D";
					} else {
						anio = elem.get(i + 3).substring(0, 4);
					}
					cont++;
				}
				referencia = referencia.trim();
				masInformacion.setAnio(anio);
				masInformacion.setAutores(autores);
				masInformacion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_PROYECTO, Constantes.PROYECTO, tipoProduccion);
				masInformacion.setTipo(tipo);
				masInformacion.setGrupo(grupo);
				masInformacion.setRepetido("NO");
				identificarRepetidos(masInformacionAux, masInformacion);
				masInformacionAux.add(masInformacion);
			}
		}

		return masInformacionAux;
	}

	/*
	 * Extraer Producciones en Arte
	 */

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerObras(ArrayList<String> elem, Grupo grupo) {
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodArteAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionArte = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL PRODUCTO:")) {

				tipo = new Tipo(Constantes.ID_OBRA, Constantes.OBRA, tipoProduccion);

				// Autores
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL PRODUCTO:")
						&& !elem.get(cont).contains("INSTANCIAS DE VALORACIÓN DE LA OBRA")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("CREACIÓN:")) {
						anio = elem.get(cont + 1).substring(elem.get(cont + 1).length() - 4,
								elem.get(cont + 1).length());
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				referencia = referencia.trim();
				produccionArte.setAnio(anio);
				produccionArte.setReferencia(referencia);
				produccionArte.setTipo(tipo);
				produccionArte.setGrupo(grupo);
				produccionArte.setRepetido("NO");
				identificarRepetidos(prodArteAux, produccionArte);
				prodArteAux.add(produccionArte);
			}
		}

		return prodArteAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerRegistrosAcuerdo(ArrayList<String> elem, Grupo grupo) {
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodArteAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionArte = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL PRODUCTO:")) {

				tipo = new Tipo(Constantes.ID_ACUERDO_LICENCIA, Constantes.ACUERDO_LICENCIA, tipoProduccion);

				// Autores
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL PRODUCTO:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("CREACIÓN:")) {
						anio = elem.get(cont + 1).substring(elem.get(cont + 1).length() - 4,
								elem.get(cont + 1).length());
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				referencia = referencia.trim();
				produccionArte.setAnio(anio);
				produccionArte.setReferencia(referencia);
				produccionArte.setTipo(tipo);
				produccionArte.setGrupo(grupo);
				produccionArte.setRepetido("NO");
				identificarRepetidos(prodArteAux, produccionArte);
				prodArteAux.add(produccionArte);
			}
		}

		return prodArteAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerIndustrias(ArrayList<String> elem, Grupo grupo) {
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodArteAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionArte = new Produccion();

			if (elem.get(i).contains("NOMBRE DE LA EMPRESA CREATIVA:")) {

				tipo = new Tipo(Constantes.ID_INDUSTRIA_CREATIVA, Constantes.INDUSTRIA_CREATIVA, tipoProduccion);

				// Autores
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DE LA EMPRESA CREATIVA:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("COMERCIO:")) {
						anio = elem.get(cont + 1).substring(0, 4);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				referencia = referencia.trim();
				produccionArte.setAnio(anio);
				produccionArte.setReferencia(referencia);
				produccionArte.setTipo(tipo);
				produccionArte.setGrupo(grupo);
				produccionArte.setRepetido("NO");
				identificarRepetidos(prodArteAux, produccionArte);
				prodArteAux.add(produccionArte);
			}
		}

		return prodArteAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEventoArtistico(ArrayList<String> elem, Grupo grupo) {
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodArteAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionArte = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL EVENTO:")) {

				tipo = new Tipo(Constantes.ID_EVENTO_ARTISTICO, Constantes.EVENTO_ARTISTICO, tipoProduccion);

				// Autores
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL EVENTO:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("INICIO:")) {
						anio = elem.get(cont + 1).substring(0, 4);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				referencia = referencia.trim();
				produccionArte.setAnio(anio);
				produccionArte.setReferencia(referencia);
				produccionArte.setTipo(tipo);
				produccionArte.setGrupo(grupo);
				produccionArte.setRepetido("NO");
				identificarRepetidos(prodArteAux, produccionArte);
				prodArteAux.add(produccionArte);
			}
		}

		return prodArteAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerTallerCreativo(ArrayList<String> elem, Grupo grupo) {
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> prodArteAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccionArte = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL TALLER:")) {

				tipo = new Tipo(Constantes.ID_TALLER_CREATIVO, Constantes.TALLER_CREATIVO, tipoProduccion);

				// Autores
				int cont = i;
				referencia = elem.get(i);
				cont++;
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL TALLER:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("FINALIZACIÓN:")) {
						int pos = actual.indexOf("FINALIZACIÓN:");
						anio = actual.substring(pos + 14, pos + 18);
					}
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				referencia = referencia.trim();
				produccionArte.setAnio(anio);
				produccionArte.setReferencia(referencia);
				produccionArte.setTipo(tipo);
				produccionArte.setGrupo(grupo);
				produccionArte.setRepetido("NO");
				identificarRepetidos(prodArteAux, produccionArte);
				prodArteAux.add(produccionArte);
			}
		}

		return prodArteAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public void identificarRepetidosBibliograficos(ArrayList<ProduccionBibliografica> elem,
			ProduccionBibliografica produccion) {
		String referenciaAuxInterno = produccion.getReferencia();
		String anioAuxInterno = produccion.getAnio();
		String idAuxInterno = produccion.getIdentificador();
		for (int j = 0; j < elem.size(); j++) {
			String referenciaAux = elem.get(j).getReferencia();
			String anioAux = elem.get(j).getAnio();
			String idAux = elem.get(j).getIdentificador();
			if (referenciaAux.contains(referenciaAuxInterno) || referenciaAuxInterno.contains(referenciaAux)) {
				if (anioAux.contains(anioAuxInterno) || anioAuxInterno.contains(anioAux)) {
					if (idAux.contains(idAuxInterno) || idAuxInterno.contains(idAux)) {
						produccion.setRepetido("SI");
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public void identificarRepetidos(ArrayList<Produccion> elem, Produccion produccion) {
		String referenciaAuxInterno = produccion.getReferencia();
		String anioAuxInterno = produccion.getAnio();
		for (int j = 0; j < elem.size(); j++) {
			String referenciaAux = elem.get(j).getReferencia();
			String anioAux = elem.get(j).getAnio();
			if (referenciaAux.contains(referenciaAuxInterno) || referenciaAuxInterno.contains(referenciaAux)) {
				if (anioAux.contains(anioAuxInterno) || anioAuxInterno.contains(anioAux)) {
					produccion.setRepetido("SI");
				}
			}
		}
	}

	public void guardarDatos(List<Grupo> grupos) {

		manager.clear();

		for (int i = 0; i < grupoInves.size(); i++) {
			manager.getTransaction().begin();
			manager.merge(grupoInves.get(i));
			manager.getTransaction().commit();
		}

		for (int i = 0; i < grupos.size(); i++) {
			manager.getTransaction().begin();
			manager.merge(grupos.get(i));
			manager.getTransaction().commit();
		}
	}

	public void eliminarDatos() {

		manager.clear();

		@SuppressWarnings("unchecked")
		List<GruposInves> gruposInves = (ArrayList<GruposInves>) manager.createQuery("FROM GRUPOS_INVES")
				.getResultList();
		for (int i = 0; i < gruposInves.size(); i++) {
			manager.getTransaction().begin();
			manager.remove(gruposInves.get(i));
			manager.getTransaction().commit();
		}
		
		@SuppressWarnings("unchecked")
		List<Investigador> investigadores = (ArrayList<Investigador>) manager.createQuery("FROM INVESTIGADORES")
				.getResultList();
		for (int i = 0; i < investigadores.size(); i++) {
			manager.getTransaction().begin();
			manager.remove(investigadores.get(i));
			manager.getTransaction().commit();
		}

		@SuppressWarnings("unchecked")
		List<Produccion> produccionesg = (ArrayList<Produccion>) manager.createQuery("FROM PRODUCCIONESG")
				.getResultList();
		for (int i = 0; i < produccionesg.size(); i++) {
			manager.getTransaction().begin();
			manager.remove(produccionesg.get(i));
			manager.getTransaction().commit();
		}

		@SuppressWarnings("unchecked")
		List<ProduccionBibliografica> produccionesBibliograficasg = (ArrayList<ProduccionBibliografica>) manager
				.createQuery("FROM PRODUCCIONBIBLIOGRAFICAG").getResultList();
		for (int i = 0; i < produccionesBibliograficasg.size(); i++) {
			manager.getTransaction().begin();
			manager.remove(produccionesBibliograficasg.get(i));
			manager.getTransaction().commit();
		}

		@SuppressWarnings("unchecked")
		List<Grupo> grupos = (ArrayList<Grupo>) manager.createQuery("FROM GRUPOS").getResultList();
		for (int i = 0; i < grupos.size(); i++) {
			for (int j = 0; j < grupos.get(i).getLineasInvestigacion().size(); j++) {
				grupos.get(i).removeLineasInvestigacion(grupos.get(i).getLineasInvestigacion().get(j));
				j--;
			}
			manager.getTransaction().begin();
			manager.merge(grupos.get(i));
			manager.getTransaction().commit();
		}

		@SuppressWarnings("unchecked")
		List<LineasInvestigacion> lineas = (ArrayList<LineasInvestigacion>) manager
				.createQuery("FROM LINEASINVESTIGACION").getResultList();
		for (int i = 0; i < lineas.size(); i++) {
			manager.getTransaction().begin();
			manager.remove(lineas.get(i));
			manager.getTransaction().commit();
		}

	}
}