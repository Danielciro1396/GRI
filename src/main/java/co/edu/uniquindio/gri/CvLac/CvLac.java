package co.edu.uniquindio.gri.CvLac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import co.edu.uniquindio.gri.Master.Constantes;
import co.edu.uniquindio.gri.Objects.LineasInvestigacion;
import co.edu.uniquindio.gri.Objects.Tipo;
import co.edu.uniquindio.gri.Objects.TipoProduccion;

public class CvLac {

	// Lista en la que se guardan las direcciones url de cada investigador
	public ArrayList<String> urlSet;

	public String nombreInvestigadorAux;

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
	 * llamado al metodo que asigna los datos a cada investigador
	 * 
	 * @param url,
	 *            direccion url de un investigador
	 * @return
	 */
	public Investigador extraer(String url, String estado) {

		Investigador auxInvestigador = new Investigador();

		if (getStatusConnectionCode(url) == 200) {

			// Obtenemos el id del investigador a partir de la URL
			long id = Long.parseLong(url.substring(url.length() - 10));

			// Datos Generales
			ArrayList<String> elemInfoPersonal = new ArrayList<>();

			// Obtengo el HTML de la web en un objeto Document
			Document document = getHtmlDocument(url);

			// Busca todas las coincidencias que estan dentro de
			Elements entradas = document.select("tbody>tr>td>table>tbody");
			if (estado.equals("ACTUAL")) {

				for (Element elem : entradas) {

					/*
					 * Extraer Datos Personales
					 */

					if (elem.text().contains("Nombre en citaciones")) {
						elemInfoPersonal.add(elem.toString());
						elemInfoPersonal = limpiar(elemInfoPersonal);
					}

					if (elem.text().contains("Formación Académica")) {
						ArrayList<String> aux = new ArrayList<>();
						aux.add(elem.toString());
						aux = limpiar(aux);
						elemInfoPersonal.addAll(aux);
					}

					if (elem.text().contains("Líneas de investigación")) {
						ArrayList<String> aux = new ArrayList<>();
						aux.add(elem.toString());
						aux = limpiar(aux);
						elemInfoPersonal.addAll(aux);
					}
					if (elem.text().contains("Experiencia profesional")) {
						ArrayList<String> aux = new ArrayList<>();
						aux.add(elem.toString());
						aux = limpiar(aux);
						elemInfoPersonal.addAll(aux);
					}

				}

				auxInvestigador = extraerDatos(elemInfoPersonal, id, estado);

				for (Element elem : entradas) {

					/*
					 * Extraer idiomas de los investigadores
					 */

					if (elem.text().startsWith("Idiomas")) {
						ArrayList<String> elemIdiomas = new ArrayList<>();
						elemIdiomas.add(elem.toString());
						elemIdiomas = limpiar(elemIdiomas);

						List<Idiomas> auxIdiomas = auxInvestigador.getIdiomas();
						if (auxIdiomas == null) {
							auxInvestigador.setIdiomas(extraerIdiomas(elemIdiomas, auxInvestigador));
						} else {
							auxIdiomas.addAll(extraerIdiomas(elemIdiomas, auxInvestigador));
							auxInvestigador.setIdiomas(auxIdiomas);
						}
					}

					/*
					 * Extraer las Actividades de Formacion
					 */

					if (elem.text().contains("Cursos de corta duración")) {
						ArrayList<String> elemCursosCortaDuracion = new ArrayList<>();
						elemCursosCortaDuracion.add(elem.toString());
						elemCursosCortaDuracion = limpiar(elemCursosCortaDuracion);

						List<Produccion> auxFormacion = auxInvestigador.getProducciones();
						if (auxFormacion == null) {
							auxInvestigador
									.setProducciones(extraerCurosCortos(elemCursosCortaDuracion, auxInvestigador));
						} else {
							auxFormacion.addAll(extraerCurosCortos(elemCursosCortaDuracion, auxInvestigador));
							auxInvestigador.setProducciones(auxFormacion);
						}
					}

					if (elem.text().contains("Trabajos dirigidos/tutorías")) {
						ArrayList<String> elemTrabajosDirigidosTutorias = new ArrayList<>();
						elemTrabajosDirigidosTutorias.add(elem.toString());
						elemTrabajosDirigidosTutorias = limpiar(elemTrabajosDirigidosTutorias);

						List<Produccion> auxFormacion = auxInvestigador.getProducciones();
						if (auxFormacion == null) {
							auxInvestigador.setProducciones(
									extraerTrabajosTutorias(elemTrabajosDirigidosTutorias, auxInvestigador));
						} else {
							auxFormacion
									.addAll(extraerTrabajosTutorias(elemTrabajosDirigidosTutorias, auxInvestigador));
							auxInvestigador.setProducciones(auxFormacion);
						}
					}

					/*
					 * Extraer las Actividades como Evaluador
					 */

					if (elem.text().startsWith("Jurado en comités de evaluación")) {
						ArrayList<String> elemJuradoComite = new ArrayList<>();
						elemJuradoComite.add(elem.toString());
						elemJuradoComite = limpiar(elemJuradoComite);

						List<Produccion> auxActEvaluador = auxInvestigador.getProducciones();
						if (auxActEvaluador == null) {
							auxInvestigador.setProducciones(extraerJuradoComites(elemJuradoComite, auxInvestigador));
						} else {
							auxActEvaluador.addAll(extraerJuradoComites(elemJuradoComite, auxInvestigador));
							auxInvestigador.setProducciones(auxActEvaluador);
						}
					}

					if (elem.text().startsWith("Participación en comités de evaluación")) {
						ArrayList<String> elemParticipacionComite = new ArrayList<>();
						elemParticipacionComite.add(elem.toString());
						elemParticipacionComite = limpiar(elemParticipacionComite);

						List<Produccion> auxActEvaluador = auxInvestigador.getProducciones();
						if (auxActEvaluador == null) {
							auxInvestigador.setProducciones(
									extraerParticipacionComites(elemParticipacionComite, auxInvestigador));
						} else {
							auxActEvaluador
									.addAll(extraerParticipacionComites(elemParticipacionComite, auxInvestigador));
							auxInvestigador.setProducciones(auxActEvaluador);
						}
					}

					if (elem.text().contains("Par evaluador") && !elem.text().contains("reconocido por Colciencias.")) {
						ArrayList<String> elemParEvaluador = new ArrayList<>();
						elemParEvaluador.add(elem.toString());
						elemParEvaluador = limpiar(elemParEvaluador);

						List<Produccion> auxActEvaluador = auxInvestigador.getProducciones();
						if (auxActEvaluador == null) {
							auxInvestigador.setProducciones(extraerParEvaluador(elemParEvaluador, auxInvestigador));
						} else {
							auxActEvaluador.addAll(extraerParEvaluador(elemParEvaluador, auxInvestigador));
							auxInvestigador.setProducciones(auxActEvaluador);
						}
					}

					/*
					 * Extraer la Apropiacion social
					 */

					if (elem.text().startsWith("Ediciones/revisiones")) {
						ArrayList<String> elemEdicion = new ArrayList<>();
						elemEdicion.add(elem.toString());
						elemEdicion = limpiar(elemEdicion);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(extraerEdicionesRevisiones(elemEdicion, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerEdicionesRevisiones(elemEdicion, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Eventos científicos")) {
						ArrayList<String> elemEventos = new ArrayList<>();
						elemEventos.add(elem.toString());
						elemEventos = limpiar(elemEventos);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(extraerEventos(elemEventos, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerEventos(elemEventos, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Redes de conocimiento especializado")) {
						ArrayList<String> elemRedesConocimiento = new ArrayList<>();
						elemRedesConocimiento.add(elem.toString());
						elemRedesConocimiento = limpiar(elemRedesConocimiento);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(
									extraerRedesDeConocimiento(elemRedesConocimiento, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerRedesDeConocimiento(elemRedesConocimiento, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Generación de contenido impresa")) {
						ArrayList<String> elemContenidoImpreso = new ArrayList<>();
						elemContenidoImpreso.add(elem.toString());
						elemContenidoImpreso = limpiar(elemContenidoImpreso);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador
									.setProducciones(extraerContenidoImpreso(elemContenidoImpreso, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerContenidoImpreso(elemContenidoImpreso, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Generación de contenido multimedia")) {
						ArrayList<String> elemContenidoMultimedia = new ArrayList<>();
						elemContenidoMultimedia.add(elem.toString());
						elemContenidoMultimedia = limpiar(elemContenidoMultimedia);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(
									extraerContenidoMultimedia(elemContenidoMultimedia, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerContenidoMultimedia(elemContenidoMultimedia, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Generación de contenido virtual")) {
						ArrayList<String> elemContenidoVirtual = new ArrayList<>();
						elemContenidoVirtual.add(elem.toString());
						elemContenidoVirtual = limpiar(elemContenidoVirtual);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador
									.setProducciones(extraerContenidoVirtual(elemContenidoVirtual, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerContenidoVirtual(elemContenidoVirtual, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Estrategias de comunicación del conocimiento")) {
						ArrayList<String> elemEstrategiaComunicacion = new ArrayList<>();
						elemEstrategiaComunicacion.add(elem.toString());
						elemEstrategiaComunicacion = limpiar(elemEstrategiaComunicacion);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(extraerEstrategiaComunicacionPedagogica(
									elemEstrategiaComunicacion, auxInvestigador));
						} else {
							auxApropSocial.addAll(extraerEstrategiaComunicacionPedagogica(elemEstrategiaComunicacion,
									auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Estrategias pedagógicas para el fomento a la CTI")) {
						ArrayList<String> elemEstrategiaPedagogica = new ArrayList<>();
						elemEstrategiaPedagogica.add(elem.toString());
						elemEstrategiaPedagogica = limpiar(elemEstrategiaPedagogica);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(
									extraerEstrategiaComunicacionPedagogica(elemEstrategiaPedagogica, auxInvestigador));
						} else {
							auxApropSocial.addAll(
									extraerEstrategiaComunicacionPedagogica(elemEstrategiaPedagogica, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Espacios de participación ciudadana")) {
						ArrayList<String> elemParticipacionCiudadana = new ArrayList<>();
						elemParticipacionCiudadana.add(elem.toString());
						elemParticipacionCiudadana = limpiar(elemParticipacionCiudadana);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(
									extraerParticipacionCiudadana(elemParticipacionCiudadana, auxInvestigador));
						} else {
							auxApropSocial
									.addAll(extraerParticipacionCiudadana(elemParticipacionCiudadana, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					if (elem.text().startsWith("Participación ciudadana en proyectos de CTI")) {
						ArrayList<String> elemParticipacionCiudadanaCTI = new ArrayList<>();
						elemParticipacionCiudadanaCTI.add(elem.toString());
						elemParticipacionCiudadanaCTI = limpiar(elemParticipacionCiudadanaCTI);

						List<Produccion> auxApropSocial = auxInvestigador.getProducciones();
						if (auxApropSocial == null) {
							auxInvestigador.setProducciones(
									extraerParticipacionCiudadanaCTI(elemParticipacionCiudadanaCTI, auxInvestigador));
						} else {
							auxApropSocial.addAll(
									extraerParticipacionCiudadanaCTI(elemParticipacionCiudadanaCTI, auxInvestigador));
							auxInvestigador.setProducciones(auxApropSocial);
						}
					}

					/*
					 * Extraer Producciones bibliograficas
					 */

					if (elem.text().startsWith("Artículos")) {
						ArrayList<String> elemArticulos = new ArrayList<>();
						elemArticulos.add(elem.toString());
						elemArticulos = limpiar(elemArticulos);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador
									.setProduccionesBibliograficas(extraerArticulos(elemArticulos, auxInvestigador));
						} else {
							auxProdBibliografica.addAll(extraerArticulos(elemArticulos, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					if (elem.text().startsWith("Libros")) {
						ArrayList<String> elemLibros = new ArrayList<>();
						elemLibros.add(elem.toString());
						elemLibros = limpiar(elemLibros);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador.setProduccionesBibliograficas(extraerLibros(elemLibros, auxInvestigador));
						} else {
							auxProdBibliografica.addAll(extraerLibros(elemLibros, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					if (elem.text().startsWith("Capitulos de libro")) {
						ArrayList<String> elemCapLibros = new ArrayList<>();
						elemCapLibros.add(elem.toString());
						elemCapLibros = limpiar(elemCapLibros);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador
									.setProduccionesBibliograficas(extraerCapLibros(elemCapLibros, auxInvestigador));
						} else {
							auxProdBibliografica.addAll(extraerCapLibros(elemCapLibros, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					if (elem.text().startsWith("Textos en publicaciones no científicas")) {
						ArrayList<String> elemPubNoCientificas = new ArrayList<>();
						elemPubNoCientificas.add(elem.toString());
						elemPubNoCientificas = limpiar(elemPubNoCientificas);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador.setProduccionesBibliograficas(
									extraerPubNoCientificas(elemPubNoCientificas, auxInvestigador));
						} else {
							auxProdBibliografica.addAll(extraerPubNoCientificas(elemPubNoCientificas, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					if (elem.text().startsWith("Otra producción blibliográfica")) {
						ArrayList<String> elemOtraProdBibliografica = new ArrayList<>();
						elemOtraProdBibliografica.add(elem.toString());
						elemOtraProdBibliografica = limpiar(elemOtraProdBibliografica);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador.setProduccionesBibliograficas(
									extraerOtraProdBibliografica(elemOtraProdBibliografica, auxInvestigador));
						} else {
							auxProdBibliografica
									.addAll(extraerOtraProdBibliografica(elemOtraProdBibliografica, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					if (elem.text().startsWith("Documentos de trabajo")) {
						ArrayList<String> elemDocumentosTrabajo = new ArrayList<>();
						elemDocumentosTrabajo.add(elem.toString());
						elemDocumentosTrabajo = limpiar(elemDocumentosTrabajo);

						List<ProduccionBibliografica> auxProdBibliografica = auxInvestigador
								.getProduccionesBibliograficas();
						if (auxProdBibliografica == null) {
							auxInvestigador.setProduccionesBibliograficas(
									extraerOtraProdBibliografica(elemDocumentosTrabajo, auxInvestigador));
						} else {
							auxProdBibliografica
									.addAll(extraerOtraProdBibliografica(elemDocumentosTrabajo, auxInvestigador));
							auxInvestigador.setProduccionesBibliograficas(auxProdBibliografica);
						}
					}

					/*
					 * Extraer Producciones tecnicas
					 */

					if (elem.text().startsWith("Softwares")) {
						ArrayList<String> elemSoftwares = new ArrayList<>();
						elemSoftwares.add(elem.toString());
						elemSoftwares = limpiar(elemSoftwares);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerProdTecnica(elemSoftwares, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemSoftwares, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Prototipos")) {
						ArrayList<String> elemPatentes = new ArrayList<>();
						elemPatentes.add(elem.toString());
						elemPatentes = limpiar(elemPatentes);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerProdTecnica(elemPatentes, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemPatentes, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Productos tecnológicos")) {
						ArrayList<String> elemProdTecnologicos = new ArrayList<>();
						elemProdTecnologicos.add(elem.toString());
						elemProdTecnologicos = limpiar(elemProdTecnologicos);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerProdTecnica(elemProdTecnologicos, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemProdTecnologicos, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Informes de investigaci")) {
						ArrayList<String> elemInformeInvestigacion = new ArrayList<>();
						elemInformeInvestigacion.add(elem.toString());
						elemInformeInvestigacion = limpiar(elemInformeInvestigacion);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador
									.setProducciones(extraerProdTecnica(elemInformeInvestigacion, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemInformeInvestigacion, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Innovación de proceso o procedimiento")) {
						ArrayList<String> elemProcesosTecnicas = new ArrayList<>();
						elemProcesosTecnicas.add(elem.toString());
						elemProcesosTecnicas = limpiar(elemProcesosTecnicas);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerProdTecnica(elemProcesosTecnicas, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemProcesosTecnicas, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Trabajos técnicos")) {
						ArrayList<String> elemTrabajosTecnicos = new ArrayList<>();
						elemTrabajosTecnicos.add(elem.toString());
						elemTrabajosTecnicos = limpiar(elemTrabajosTecnicos);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerProdTecnica(elemTrabajosTecnicos, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemTrabajosTecnicos, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Normas y Regulaciones")) {
						ArrayList<String> elemNormasRegulaciones = new ArrayList<>();
						elemNormasRegulaciones.add(elem.toString());
						elemNormasRegulaciones = limpiar(elemNormasRegulaciones);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador
									.setProducciones(extraerProdTecnica(elemNormasRegulaciones, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerProdTecnica(elemNormasRegulaciones, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					if (elem.text().startsWith("Empresas de base tecnológica")) {
						ArrayList<String> elemEmpresasTecnologicas = new ArrayList<>();
						elemEmpresasTecnologicas.add(elem.toString());
						elemEmpresasTecnologicas = limpiar(elemEmpresasTecnologicas);

						List<Produccion> auxProdTecnica = auxInvestigador.getProducciones();
						if (auxProdTecnica == null) {
							auxInvestigador.setProducciones(extraerEmpresas(elemEmpresasTecnologicas, auxInvestigador));
						} else {
							auxProdTecnica.addAll(extraerEmpresas(elemEmpresasTecnologicas, auxInvestigador));
							auxInvestigador.setProducciones(auxProdTecnica);
						}
					}

					/*
					 * Extraer mas informacion
					 */

					if (elem.text().startsWith("Demás trabajos")) {
						ArrayList<String> elemDemasTrabajos = new ArrayList<>();
						elemDemasTrabajos.add(elem.toString());
						elemDemasTrabajos = limpiar(elemDemasTrabajos);

						List<Produccion> auxProduccion = auxInvestigador.getProducciones();
						if (auxProduccion == null) {
							auxInvestigador.setProducciones(extraerDemasTrabajos(elemDemasTrabajos, auxInvestigador));
						} else {
							auxProduccion.addAll(extraerDemasTrabajos(elemDemasTrabajos, auxInvestigador));
							auxInvestigador.setProducciones(auxProduccion);
						}
					}

					if (elem.text().startsWith("Proyectos")) {
						ArrayList<String> elemProyectos = new ArrayList<>();
						elemProyectos.add(elem.toString());
						elemProyectos = limpiar(elemProyectos);

						List<Produccion> auxProduccion = auxInvestigador.getProducciones();
						if (auxProduccion == null) {
							auxInvestigador.setProducciones(extraerProyectos(elemProyectos, auxInvestigador));
						} else {
							auxProduccion.addAll(extraerProyectos(elemProyectos, auxInvestigador));
							auxInvestigador.setProducciones(auxProduccion);
						}
					}

					/*
					 * Extraer Producciones en Arte
					 */

					if (elem.text().startsWith("Obras o productos")) {
						ArrayList<String> elemObrasProductos = new ArrayList<>();
						elemObrasProductos.add(elem.toString());
						elemObrasProductos = limpiar(elemObrasProductos);

						List<Produccion> auxProdArte = auxInvestigador.getProducciones();
						if (auxProdArte == null) {
							auxInvestigador.setProducciones(extraerObras(elemObrasProductos, auxInvestigador));
						} else {
							auxProdArte.addAll(extraerObras(elemObrasProductos, auxInvestigador));
							auxInvestigador.setProducciones(auxProdArte);
						}
					}

					if (elem.text().startsWith("Registros de acuerdo de licencia")) {
						ArrayList<String> elemRegistrosLicencia = new ArrayList<>();
						elemRegistrosLicencia.add(elem.toString());
						elemRegistrosLicencia = limpiar(elemRegistrosLicencia);

						List<Produccion> auxProdArte = auxInvestigador.getProducciones();
						if (auxProdArte == null) {
							auxInvestigador
									.setProducciones(extraerRegistrosAcuerdo(elemRegistrosLicencia, auxInvestigador));
						} else {
							auxProdArte.addAll(extraerRegistrosAcuerdo(elemRegistrosLicencia, auxInvestigador));
							auxInvestigador.setProducciones(auxProdArte);
						}
					}

					if (elem.text().startsWith("Industrias Creativas y culturales")) {
						ArrayList<String> elemIndustriasCreativas = new ArrayList<>();
						elemIndustriasCreativas.add(elem.toString());
						elemIndustriasCreativas = limpiar(elemIndustriasCreativas);

						List<Produccion> auxProdArte = auxInvestigador.getProducciones();
						if (auxProdArte == null) {
							auxInvestigador
									.setProducciones(extraerIndustrias(elemIndustriasCreativas, auxInvestigador));
						} else {
							auxProdArte.addAll(extraerIndustrias(elemIndustriasCreativas, auxInvestigador));
							auxInvestigador.setProducciones(auxProdArte);
						}
					}

					if (elem.text().startsWith("Eventos artísticos")) {
						ArrayList<String> elemEventoArtistico = new ArrayList<>();
						elemEventoArtistico.add(elem.toString());
						elemEventoArtistico = limpiar(elemEventoArtistico);

						List<Produccion> auxProdArte = auxInvestigador.getProducciones();
						if (auxProdArte == null) {
							auxInvestigador
									.setProducciones(extraerEventoArtistico(elemEventoArtistico, auxInvestigador));
						} else {
							auxProdArte.addAll(extraerEventoArtistico(elemEventoArtistico, auxInvestigador));
							auxInvestigador.setProducciones(auxProdArte);
						}
					}

					if (elem.text().startsWith("Talleres Creativos")) {
						ArrayList<String> elemTalleresCreativos = new ArrayList<>();
						elemTalleresCreativos.add(elem.toString());
						elemTalleresCreativos = limpiar(elemTalleresCreativos);

						List<Produccion> auxProdArte = auxInvestigador.getProducciones();
						if (auxProdArte == null) {
							auxInvestigador
									.setProducciones(extraerTallerCreativo(elemTalleresCreativos, auxInvestigador));
						} else {
							auxProdArte.addAll(extraerTallerCreativo(elemTalleresCreativos, auxInvestigador));
							auxInvestigador.setProducciones(auxProdArte);
						}
					}
				}
			} else {
				for (Element elem : entradas) {
					if (elem.text().contains("Nombre en citaciones")) {
						elemInfoPersonal.add(elem.toString());
						elemInfoPersonal = limpiar(elemInfoPersonal);
					}
				}
				auxInvestigador = extraerDatos(elemInfoPersonal, id, estado);
			}

		} else {
			System.out.println("El Status Code no es OK es: " + getStatusConnectionCode(url));
		}
		return auxInvestigador;
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
			temporal = temporal.replaceAll("&AMP;", "&");
			temporal = temporal.replaceAll("'", "");
		}
		char[] auxiliar = temporal.toCharArray();
		int posI = 0;
		int posF = 0;
		for (int j = 0; j < auxiliar.length; j++) {
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
			if (!aux.get(i).equalsIgnoreCase(" ")) {
				temporal = "";
				temporal = aux.get(i).substring(1);
				aux2.add(temporal.trim());
			}

		}
		aux.clear();

		for (int i = 0; i < aux2.size(); i++) {
			temporal = aux2.get(i);
			if (!temporal.equalsIgnoreCase("")) {
				aux.add(temporal.trim().toUpperCase());
			}

		}

		elementosLimpio = aux;
		return elementosLimpio;
	}

	/**
	 * Metodo Utilizado para extraer los autores de una publicacion, en casos
	 * especiales donde la extraccion no es clara
	 * 
	 * @param autores,
	 *            Cadena donde se encuentran los nombres de los autores
	 * @return Cadena con el nombre de los autores
	 */
	public String verificarAutores(String autores) {
		String autoresFinal = "";
		String[] aux = autores.split(",");
		for (int j = 0; j < aux.length; j++) {
			int espacios = StringUtils.countMatches(aux[j], " ");
			if (espacios >= 1 && espacios <= 3) {
				autoresFinal += ", " + aux[j];
			}
		}
		if (autoresFinal.equals("")) {
			autoresFinal = nombreInvestigadorAux;
		} else {
			autoresFinal = autoresFinal.substring(2);
		}
		return autoresFinal;
	}

	/*
	 * Extraer Datos Personales
	 */

	/**
	 * Metodo en el cual se crea un investigador con los datos extraidos de la
	 * pagina
	 * 
	 * @param elemInfoPersonal,
	 *            Lista con la informacion personal del investigador
	 * @param id,
	 *            identificador el cual se relaciona con los digitos numericos que
	 *            aparecen en la parte final del link del CvLac del investigador
	 * @return Un investigador con sus datos personales
	 */
	public Investigador extraerDatos(ArrayList<String> elemInfoPersonal, long id, String estado) {

		boolean pertenece = false;

		Investigador investigador = new Investigador();

		investigador.setId(id);

		// Si no posee datos personales, el perfil es privado.
		if (elemInfoPersonal.size() == 0) {
			investigador.setNombre("PERFIL PRIVADO");
			nombreInvestigadorAux = investigador.getNombre();
			investigador.setId(id);
			investigador.setCategoria("SIN CATEGORÍA");
			investigador.setNivelAcademico("NO ESPECIFICADO");
			investigador.setPertenencia("NO ESPECIFICADO");

		} else {
			try {

				ArrayList<LineasInvestigacion> lineas = new ArrayList<>();
				String nomLinea = "";

				for (int i = 0; i < elemInfoPersonal.size(); i++) {

					// Extraccion de la categoria del investigador

					if (elemInfoPersonal.get(i).startsWith("CATEGORÍA")) {
						String registro = elemInfoPersonal.get(i + 1);
						String categoria = registro.substring(0, registro.indexOf('(') - 1);
						investigador.setCategoria(categoria);
					}

					// Extraccion del nombre del investigador

					if (elemInfoPersonal.get(i).equals("NOMBRE")) {
						investigador.setNombre(elemInfoPersonal.get(i + 1));
						nombreInvestigadorAux = investigador.getNombre();
					}

					// Extraccion de la formacion academica
					if (elemInfoPersonal.get(i).startsWith("FORMACIÓN ACADÉMICA")) {
						investigador.setNivelAcademico(elemInfoPersonal.get(i + 1));
					}

					try {
						if (elemInfoPersonal.size() >= i && elemInfoPersonal.get(i).contains(",")
								&& elemInfoPersonal.get(i + 2).equals("SI")) {
							nomLinea = elemInfoPersonal.get(i).substring(0, elemInfoPersonal.get(i).length() - 1);
							LineasInvestigacion lineaInvestigacion = new LineasInvestigacion();
							lineaInvestigacion.setNombre(nomLinea);
							lineas.add(lineaInvestigacion);
						}
					} catch (Exception e) {
						e.getStackTrace();
					}

					try {
						if (estado.equals("ACTUAL")) {
							if (elemInfoPersonal.get(i).equals("UNIVERSIDAD DEL QUINDIO UNIQUINDIO")
									&& elemInfoPersonal.get(i + 2).contains("ACTUAL")) {
								pertenece = true;
								investigador.setPertenencia("INVESTIGADOR INTERNO");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (investigador.getCategoria() == null) {
					investigador.setCategoria("SIN CATEGORÍA");
				}

				if (investigador.getNivelAcademico() == null) {
					investigador.setNivelAcademico("NO ESPECIFICADO");
				}

				investigador.setLineasInvestigacion(lineas);

				if (estado.equals("ACTUAL")) {
					if (pertenece == false) {
						investigador.setPertenencia("INVESTIGADOR EXTERNO");
					}
				} else {
					investigador.setPertenencia("N/D");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return investigador;

	}

	/**
	 * Metodo que extrae los idiomas con los que el investigador esta familiarizado
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los idiomas del investigador
	 * @return Lista de los idiomas del investigador
	 */
	public ArrayList<Idiomas> extraerIdiomas(ArrayList<String> elem, Investigador investigador) {

		ArrayList<Idiomas> idiomasAux = new ArrayList<>();

		for (int i = 5; i < elem.size(); i++) {
			try {
				Idiomas idioma = new Idiomas();

				idioma.setIdioma(elem.get(i));
				idioma.setHabla(elem.get(i + 1));
				idioma.setEscribe(elem.get(i + 2));
				idioma.setLee(elem.get(i + 3));
				idioma.setEntiende(elem.get(i + 4));
				idioma.setInvestigador(investigador);
				idiomasAux.add(idioma);

			} catch (Exception e) {
				Idiomas idioma = new Idiomas();

				idioma.setIdioma("N/D");
				idioma.setHabla("N/D");
				idioma.setEscribe("N/D");
				idioma.setLee("N/D");
				idioma.setEntiende("N/D");
				idioma.setInvestigador(investigador);
				idiomasAux.add(idioma);

			}
			i = i + 4;

		}
		return idiomasAux;
	}

	/*
	 * Extraer las Actividades de Formacion
	 */

	/**
	 * Metodo que extrae los cursos de corta duracion realizador por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los cursos de corta duracion
	 * @return Lista de actividades de formacion
	 */
	public ArrayList<Produccion> extraerCurosCortos(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			if (elem.get(i).startsWith("CURSOS DE CORTA DURACIÓN")) {
				for (int j = i; j < elem.size(); j++) {
					if (elem.get(j).startsWith("TRABAJOS DIRIGIDOS/TUTORÍAS")) {
						break;
					}
					Produccion produccion = new Produccion();
					if (elem.get(j).contains("PRODUCCIÓN TÉCNICA - CURSOS DE CORTA DURACIÓN DICTADOS -")) {
						autores = elem.get(j + 1).substring(0, elem.get(j + 1).indexOf(","));
					}

					if (elem.get(j).contains("FINALIDAD:")) {
						int posAux = elem.get(j + 1).indexOf("EN: ");
						String cadenaAux = elem.get(j + 1).substring(posAux);
						int posCorte = cadenaAux.indexOf(",");
						anio = cadenaAux.substring(posCorte + 1, posCorte + 5);
					}
					if (elem.get(j).contains("FINALIDAD:")) {
						referencia = elem.get(j + 1);
						if (!StringUtils.isNumeric(anio)) {
							anio = "N/D";
						}

						produccion.setAutores(autores);
						produccion.setAnio(anio);
						produccion.setReferencia(referencia);
						TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_FORMACION,
								Constantes.FORMACION);
						Tipo tipo = new Tipo(Constantes.ID_CURSO_CORTO, Constantes.CURSO_CORTO, tipoProduccion);
						produccion.setTipo(tipo);
						produccion.setInvestigador(investigador);
						produccion.setRepetido("NO");
						identificarRepetidos(produccionAux, produccion);
						produccionAux.add(produccion);
					}
				}
			}
		}
		return produccionAux;
	}

	/**
	 * Metodo que extrae los Trabajos dirigidos y las tutorias del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los Trabajos dirigidos y las
	 *            tutorias
	 * @return Lista de actividades de formacion
	 */

	public ArrayList<Produccion> extraerTrabajosTutorias(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_FORMACION, Constantes.FORMACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("TRABAJOS DIRIGIDOS/TUTORÍAS - ")) {
				Produccion produccion = new Produccion();

				if (elem.get(i).contains("TRABAJOS DE GRADO DE PREGRADO")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_P, Constantes.TRABAJO_GRADO_P, tipoProduccion);

				} else if (elem.get(i).contains("TRABAJO DE GRADO DE MAESTRÍA")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_M, Constantes.TRABAJO_GRADO_M, tipoProduccion);

				} else if (elem.get(i).contains("TESIS DE DOCTORADO")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_GRADO_D, Constantes.TRABAJO_GRADO_D, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_TUTORIA, Constantes.TUTORIA, tipoProduccion);

				}
				String aux = elem.get(i + 1).substring(0, elem.get(i + 1).lastIndexOf(",")).replaceAll(", ", ",");
				anio = aux.substring(aux.length() - 4, aux.length());
				autores = verificarAutores(aux);
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("TRABAJOS DIRIGIDOS/TUTORÍAS - ")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccion.setAutores(autores);
				produccion.setAnio(anio);
				produccion.setReferencia(referencia);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/*
	 * Extraer las Actividades como Evaluador
	 */

	/**
	 * Metodo que extrae las actividades como jurado en comites de evaluacion del
	 * investigador
	 * 
	 * @param elem,
	 *            Lista elementos que contiene las actividades como jurado en
	 *            comites de evaluacion
	 * @return Lista de actividades como evaluador
	 */
	public ArrayList<Produccion> extraerJuradoComites(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_EVALUADOR, Constantes.EVALUADOR);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			try {

				if (elem.get(i).startsWith("JURADO EN COMITÉS DE EVALUACIÓN")) {

					for (int j = i; j < elem.size(); j++) {

						Produccion produccion = new Produccion();

						if (elem.get(j).startsWith("PARTICIPACIÓN EN COMITÉS DE EVALUACIÓN")) {
							break;
						} else if (elem.get(j).startsWith("DATOS COMPLEMENTARIOS")) {
							j++;
							autores = elem.get(j).substring(0, elem.get(j).length() - 1);
						} else if (elem.get(j).startsWith("TITULO:")) {
							referencia = "";
							for (int k = j; k < elem.size(); k++) {
								if (elem.get(k).startsWith("DATOS COMPLEMENTARIOS")
										|| elem.get(k).startsWith("AREAS:")) {
									break;
								}
								if (elem.get(k).endsWith(":")) {
									referencia += elem.get(k) + " ";
								} else {
									referencia += elem.get(k) + ", ";
								}
								j = k;
							}

							if (referencia.endsWith(",, ")) {
								referencia = referencia.substring(0, referencia.length() - 3);
							} else {
								referencia = referencia.substring(0, referencia.length() - 2);
							}

							anio = "N/D";

							produccion.setAutores(autores);
							produccion.setReferencia(referencia);
							produccion.setAnio(anio);
							Tipo tipo = new Tipo(Constantes.ID_JURADO_COMITE_EVALUADOR,
									Constantes.JURADO_COMITE_EVALUADOR, tipoProduccion);
							produccion.setTipo(tipo);
							produccion.setInvestigador(investigador);
							produccion.setRepetido("NO");
							identificarRepetidos(produccionAux, produccion);
							produccionAux.add(produccion);
							i = j;
						}

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae las actividades de Participacion en comites de evaluacion
	 * del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las actividades de Participacion
	 *            en comites de evaluacion
	 * @return Lista de actividades como evaluador
	 */
	public ArrayList<Produccion> extraerParticipacionComites(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_EVALUADOR, Constantes.EVALUADOR);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			try {

				if (elem.get(i).startsWith("PARTICIPACIÓN EN COMITÉS DE EVALUACIÓN")) {
					for (int j = i; j < elem.size(); j++) {

						Produccion produccion = new Produccion();

						if (elem.get(i).startsWith("PAR EVALUADOR")) {
							break;
						} else if (elem.get(j).startsWith("DATOS COMPLEMENTARIOS")) {
							j++;
							autores = elem.get(j).substring(0, elem.get(j).indexOf(','));
							referencia = elem.get(j).substring(elem.get(j).indexOf(',') + 2) + " ";

							for (int k = j + 1; k < elem.size(); k++) {
								if (elem.get(k).startsWith("DATOS COMPLEMENTARIOS") || elem.get(k).startsWith("AREAS:")
										|| elem.get(k).startsWith("PAR EVALUADOR")) {
									break;
								}
								if (elem.get(k).endsWith(":")) {
									referencia += elem.get(k) + " ";
								} else {
									referencia += elem.get(k) + ", ";
								}
								j = k;
							}

							referencia = referencia.substring(0, referencia.length() - 2);
							anio = "N/D";

							produccion.setAutores(autores);
							produccion.setReferencia(referencia);
							produccion.setAnio(anio);
							Tipo tipo = new Tipo(Constantes.ID_PARTICIPACION_COMITE_EVALUADOR,
									Constantes.PARTICIPACION_COMITE_EVALUADOR, tipoProduccion);
							produccion.setTipo(tipo);
							produccion.setInvestigador(investigador);
							produccion.setRepetido("NO");
							identificarRepetidos(produccionAux, produccion);
							produccionAux.add(produccion);
							i = j;
						}

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae las actividades como Par evaluador del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las actividades como Par evaluador
	 * @return Lista de actividades como evaluador
	 */
	public ArrayList<Produccion> extraerParEvaluador(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_EVALUADOR, Constantes.EVALUADOR);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			try {

				if (elem.get(i).startsWith("PAR EVALUADOR") && elem.get(i + 1).startsWith("ÁMBITO:")) {
					for (int j = i + 1; j < elem.size(); j++) {

						Produccion produccion = new Produccion();

						if (elem.get(j).startsWith("ÁMBITO:")) {

							referencia = elem.get(j) + " ";

							for (int k = j + 1; k < elem.size(); k++) {

								if (elem.get(k).startsWith("ÁMBITO:")) {
									break;
								}

								if (elem.get(k).startsWith("INSTITUCIÓN:")) {

									anio = elem.get(k + 1).substring(elem.get(k + 1).indexOf(',') + 2,
											elem.get(k + 1).indexOf(',') + 6);
								}
								if (elem.get(k).endsWith(":")) {
									referencia += elem.get(k) + " ";
								} else {
									referencia += elem.get(k) + ", ";
								}

								j = k;
							}

							referencia = referencia.substring(0, referencia.length() - 2);

							if (!StringUtils.isNumeric(anio)) {
								anio = "N/D";
							}

							produccion.setAutores(autores);
							produccion.setReferencia(referencia);
							produccion.setAnio(anio);
							Tipo tipo = new Tipo(Constantes.ID_PAR_EVALUADOR, Constantes.PAR_EVALUADOR, tipoProduccion);
							produccion.setTipo(tipo);
							produccion.setInvestigador(investigador);
							produccion.setRepetido("NO");
							identificarRepetidos(produccionAux, produccion);
							produccionAux.add(produccion);
							i = j;
						}

					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return (produccionAux);
	}

	/*
	 * Extraer la Apropiacion social
	 */

	/**
	 * Metodo que extrae las actividades de edicion y revision del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las actividades de edicion y
	 *            revision
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerEdicionesRevisiones(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).startsWith("PRODUCCIÓN TÉCNICA - EDITORACIÓN O REVISIÓN")) {
				autores = elem.get(i + 1).substring(0, elem.get(i + 1).indexOf(","));
				referencia = elem.get(i + 1).substring(elem.get(i + 1).indexOf(",") + 2,
						elem.get(i + 1).lastIndexOf(","));
			}
			if (elem.get(i).contains("EN: ")) {
				anio = elem.get(i).substring(elem.get(i).lastIndexOf(",") - 4, elem.get(i).lastIndexOf(","));
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_EDICION, Constantes.EDICION, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}

		}

		return (produccionAux);
	}

	/**
	 * Metodo que extrae los eventos en los que ha participado el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los eventos
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerEventos(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL EVENTO:")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL EVENTO:")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:") && !elem.get(cont).contains("PRODUCTOS ASOCIADOS")
						&& !elem.get(cont).contains("NOMBRE DEL PRODUCTO:")
						&& !elem.get(cont).contains("TIPO DE PRODUCTO:")
						&& !elem.get(cont).contains("INSTITUCIONES ASOCIADAS")
						&& !elem.get(cont).contains("PARTICIPANTES") && !StringUtils.isNumeric(elem.get(cont))) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("REALIZADO EL:")) {
				anio = elem.get(i).substring(elem.get(i).indexOf("-") - 4, elem.get(i).indexOf("-"));
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
			}
			autores = "";
			if (elem.get(i).contains("PARTICIPANTES")) {
				int cont = i + 1;
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL EVENTO:")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:") && !elem.get(cont).contains("PARTICIPANTES")
						&& !StringUtils.isNumeric(elem.get(cont))) {
					String actual = elem.get(cont);
					autores += " " + actual;
					cont++;
				}
				autores = autores.replace("NOMBRE:", "");
				autores = autores.trim();
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_EVENTO_CINTIFICO, Constantes.EVENTO_CIENTIFICO, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}

		}

		return (produccionAux);
	}

	/**
	 * Metodo que extrae las Redes de conocimiento del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las Redes de conocimiento
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerRedesDeConocimiento(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("NOMBRE DE LA RED")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DE LA RED")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("CREADA EL:")) {
				anio = elem.get(i).substring(elem.get(i).indexOf("-") - 4, elem.get(i).indexOf("-"));
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_RED, Constantes.RED, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae el contenido impreso generado por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene el contenido impreso
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerContenidoImpreso(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			Produccion produccion = new Produccion();
			if (elem.get(i).contains("NOMBRE")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE") && !elem.get(cont).contains("PALABRAS:")
						&& !elem.get(cont).contains("SECTORES:") && !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("EN LA FECHA")) {

				if (elem.get(i).contains("-")) {
					anio = elem.get(i).substring(elem.get(i).indexOf("-") - 4, elem.get(i).indexOf("-"));
				}

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_CONTENIDO_IMPRESO, Constantes.CONTENIDO_IMPRESO, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae el contenido multimedia generado por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene el contenido multimedia
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerContenidoMultimedia(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("PRODUCCIÓN TÉCNICA - MULTIMEDIA -")) {
				Produccion produccion = new Produccion();
				String aux = elem.get(i + 1).substring(0, elem.get(i + 1).lastIndexOf(",")).replaceAll(", ", ",");
				anio = aux.substring(aux.length() - 4, aux.length());
				autores = verificarAutores(aux);
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN TÉCNICA - MULTIMEDIA -")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.substring(1);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_CONTENIDO_MULTIMEDIA, Constantes.CONTENIDO_MULTIMEDIA,
						tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae el contenido virtual generado por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene el contenido virtual
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerContenidoVirtual(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("NOMBRE")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE") && !elem.get(cont).contains("PALABRAS:")
						&& !elem.get(cont).contains("SECTORES:") && !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains(", EN 1") || elem.get(i).contains(", EN 2")) {
				anio = elem.get(i).substring(elem.get(i).lastIndexOf("-") - 7, elem.get(i).lastIndexOf("-") - 3);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_CONTENIDO_VIRTUAL, Constantes.CONTENIDO_VIRTUAL, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae las Estrategias de comunicacion y las Estrategias
	 * pedagogicas generadas por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las Estrategias de comunicacion o
	 *            las Estrategias pedagogicas
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerEstrategiaComunicacionPedagogica(ArrayList<String> elem,
			Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";
		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("ESTRATEGIAS DE COMUNICACIÓN DEL CONOCIMIENTO")) {

				tipo = new Tipo(Constantes.ID_ESTRATEGIA_COMUNICACION, Constantes.ESTRATEGIA_COMUNICACION,
						tipoProduccion);

			} else if (elem.get(i).contains("ESTRATEGIAS PEDAGÓGICAS PARA EL FOMENTO A LA CTI")) {

				tipo = new Tipo(Constantes.ID_ESTRATEGIA_PEDAGOGICA, Constantes.ESTRATEGIA_PEDAGOGICA, tipoProduccion);

			}
			if (elem.get(i).contains("NOMBRE DE LA ESTRATEGIA")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DE LA ESTRATEGIA")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("FINALIZÓ EN :")) {
				anio = elem.get(i).substring(elem.get(i).lastIndexOf(",") - 4, elem.get(i).lastIndexOf(","));
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae la Participacion ciudadana del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las Participaciones ciudadanas
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerParticipacionCiudadana(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL ESPACIO")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL ESPACIO")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("REALIZADO EL:")) {
				anio = elem.get(i).substring(elem.get(i).indexOf(",") + 2, elem.get(i).indexOf(",") + 6);
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_ESPACIO_PARTICIPACION, Constantes.ESPACIO_PARTICIPACION,
						tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae la Participacion ciudadana CTI del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las Participaciones ciudadanas en
	 *            proyectos de CTI
	 * @return Lista de actividades de apropiacion social
	 */
	public ArrayList<Produccion> extraerParticipacionCiudadanaCTI(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String anio = "";
		String referencia = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_APROPIACION, Constantes.APROPIACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			Produccion produccion = new Produccion();

			if (elem.get(i).contains("NOMBRE DEL PROYECTO")) {
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("NOMBRE DEL PROYECTO")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					cont++;
				}
				referencia = referencia.trim();
			}
			if (elem.get(i).contains("FINALIZÓ EN :")) {
				anio = elem.get(i + 1).substring(elem.get(i + 1).lastIndexOf(",") - 4,
						elem.get(i + 1).lastIndexOf(","));
				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autores);
				produccion.setReferencia(referencia);
				produccion.setAnio(anio);
				Tipo tipo = new Tipo(Constantes.ID_ESPACIO_PARTICIPACION_CTI, Constantes.ESPACIO_PARTICIPACION_CTI,
						tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/*
	 * Extraer Producciones bibliograficas
	 */

	/**
	 * Metodo que extrae los articulos que publico el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los articulos
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerArticulos(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String referencia = "";
		String anio = "";
		String issn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("PRODUCCIÓN BIBLIOGRÁFICA")) {

				ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

				if (elem.get(i).contains("PUBLICADO EN REVISTA ESPECIALIZADA")) {

					tipo = new Tipo(Constantes.ID_ARTICULO, Constantes.ARTICULO, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_OTRO_ARTICULO, Constantes.OTRO_ARTICULO, tipoProduccion);

				}
				// Autores
				String general = elem.get(i + 1);
				int inicio = general.indexOf("\"");
				try {
					autores = general.substring(0, inicio - 2);
				} catch (Exception e) {
					autores = "N/D";
					System.err.println(investigador.getNombre() + " " + investigador.getId());
				}

				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN BIBLIOGRÁFICA")
						&& !elem.get(cont).contains("PALABRAS") && !elem.get(cont).contains("SECTORES")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("ISSN:")) {
						if (elem.get(cont + 1).contains("ED:")) {
							issn = "N/D";
						} else {
							issn = elem.get(cont + 1);
						}
					} else if (actual.contains("FASC.")) {
						anio = elem.get(cont + 1).substring(elem.get(cont + 1).indexOf(",") + 1,
								elem.get(cont + 1).lastIndexOf(","));
					}
					cont++;
				}
				referencia = referencia.trim();
				if (referencia.endsWith("DOI:")) {
					referencia = referencia.substring(0, referencia.length() - 6);
				}

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(issn);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setInvestigador(investigador);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return (prodBibliograficaAux);
	}

	/**
	 * Metodo que extrae los libros que publico el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los libros
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerLibros(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String referencia = "";
		String anio = "";
		String isbn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();
		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("PRODUCCIÓN BIBLIOGRÁFICA")) {
				ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();
				if (elem.get(i).contains("LIBRO RESULTADO DE INVESTIGACIÓN")) {

					tipo = new Tipo(Constantes.ID_LIBRO, Constantes.LIBRO, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_OTRO_LIBRO, Constantes.OTRO_LIBRO, tipoProduccion);

				}
				// Autores
				String general = elem.get(i + 1);
				int inicio = general.indexOf("\"");
				autores = general.substring(0, inicio - 2);
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN BIBLIOGRÁFICA")
						&& !elem.get(cont).contains("PALABRA:S") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("ED:")) {
						int pos = actual.indexOf("ED:");
						anio = actual.substring(pos - 6, pos - 2);

					} else if (actual.contains("ISBN:")) {
						if (elem.get(cont + 1).contains("V.")) {
							isbn = "N/D";
						} else {
							isbn = elem.get(cont + 1);
						}
					}
					cont++;
				}
				referencia = referencia.trim();

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(isbn);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setInvestigador(investigador);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return (prodBibliograficaAux);
	}

	/**
	 * Metodo que extrae los capitulos de libro que publico el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los capitulos de libro
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerCapLibros(ArrayList<String> elem, Investigador investigador) {
		String autores = "";
		String referencia = "";
		String anio = "";
		String isbn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).startsWith("TIPO:")) {
				ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();
				if (!elem.get(i).contains("OTRO CAPÍTULO")) {

					tipo = new Tipo(Constantes.ID_CAPITULO_LIBRO, Constantes.CAPITULO_LIBRO, tipoProduccion);

				} else {

					tipo = new Tipo(Constantes.ID_OTRO_CAPITULO_LIBRO, Constantes.OTRO_CAPITULO_LIBRO, tipoProduccion);

				}
				// Autores
				autores = "";
				int ref = i + 1;
				String general = "";
				do {
					general = elem.get(ref);
					int fin = general.indexOf(",");
					autores = autores + " " + general.substring(0, fin + 1);
					ref++;
				} while (!general.contains("\""));

				int cont = ref - 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).startsWith("TIPO:")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					if (actual.contains("\"")) {
						int pos = actual.indexOf(",");
						actual = actual.substring(pos);
					}
					referencia = referencia + " " + actual;
					if (actual.contains("P.")) {
						int pos = actual.lastIndexOf(",");
						anio = actual.substring(pos + 1, actual.length());

					} else if (actual.contains("ISBN:")) {
						if (elem.get(cont + 1).contains("ED:")) {
							isbn = "N/D";
						} else {
							isbn = elem.get(cont + 1);
						}
					}
					cont++;
				}
				autores = autores.substring(1, autores.length() - 1);
				referencia = autores + referencia;

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(isbn);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setInvestigador(investigador);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return (prodBibliograficaAux);
	}

	/**
	 * Metodo que extrae las publicaciones no cientificas que publico el
	 * investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las publicaciones no cientificas
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerPubNoCientificas(ArrayList<String> elem,
			Investigador investigador) {
		String autores = "";
		String referencia = "";
		String anio = "";
		String issn = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("PRODUCCIÓN BIBLIOGRÁFICA")) {
				ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();

				Tipo tipo = new Tipo(Constantes.ID_ARTICULO_NO_CIENTIFICO, Constantes.ARTICULO_NO_CIENTIFICO,
						tipoProduccion);

				// Autores
				String general = elem.get(i + 1);
				int inicio = general.indexOf("\"");
				autores = general.substring(0, inicio - 2);
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN BIBLIOGRÁFICA")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("EN:")) {
						int pos = actual.indexOf("EN:");
						String aux = actual.substring(pos);
						int ref = aux.indexOf(".");
						anio = aux.substring(ref + 2, ref + 6);

					} else if (actual.contains("ISSN:")) {
						if (elem.get(cont + 1).contains("P.")) {
							issn = "N/D";
						} else {
							issn = elem.get(cont + 1);
						}
					}
					cont++;
				}
				referencia = referencia.trim();

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(issn);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setInvestigador(investigador);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return (prodBibliograficaAux);
	}

	/**
	 * Metodo que extrae otras publicaciones bibliograficas que publico el
	 * investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene otras publicaciones bibliograficas
	 * @return Lista de Producciones bibliograficas
	 */
	public ArrayList<ProduccionBibliografica> extraerOtraProdBibliografica(ArrayList<String> elem,
			Investigador investigador) {
		String autores = "";
		String referencia = "";
		String anio = "";
		String id = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_BIBLIOGRAFICA, Constantes.BIBLIOGRAFICA);

		Tipo tipo = new Tipo();

		ArrayList<ProduccionBibliografica> prodBibliograficaAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("PRODUCCIÓN BIBLIOGRÁFICA")) {
				ProduccionBibliografica produccionBibliografica = new ProduccionBibliografica();
				if (elem.get(i).contains("DOCUMENTO DE TRABAJO")) {
					id = "DOCUMENTO DE TRABAJO";

					tipo = new Tipo(Constantes.ID_DOCUMENTO_TRABAJO, Constantes.DOCUMENTO_TRABAJO, tipoProduccion);

				} else {
					id = "OTRA PROD. BIBLIOGRAFICA";

					tipo = new Tipo(Constantes.ID_OTRA_PRODUCCION_BIBLIOGRAFICA,
							Constantes.OTRA_PRODUCCION_BIBLIOGRAFICA, tipoProduccion);

				}
				// Autores
				String general = elem.get(i + 1);
				int inicio = general.indexOf("\"");
				autores = general.substring(0, inicio - 2);
				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN BIBLIOGRÁFICA")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("SECTORES:")
						&& !elem.get(cont).contains("AREAS:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("EN:")) {
						int pos = actual.indexOf("EN:");
						String aux = actual.substring(pos);
						int ref = aux.indexOf(".");
						anio = aux.substring(ref + 2, ref + 6);

					}
					cont++;
				}
				referencia = referencia.trim();

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccionBibliografica.setReferencia(referencia);
				produccionBibliografica.setAutores(autores);
				produccionBibliografica.setIdentificador(id);
				produccionBibliografica.setTipo(tipo);
				produccionBibliografica.setAnio(anio);
				produccionBibliografica.setInvestigador(investigador);
				produccionBibliografica.setRepetido("NO");
				identificarRepetidosBibliograficos(prodBibliograficaAux, produccionBibliografica);
				prodBibliograficaAux.add(produccionBibliografica);
			}
		}

		return (prodBibliograficaAux);
	}

	/*
	 * Extraer Producciones tecnicas
	 */

	/**
	 * Metodo que extrae las producciones tecnicas del investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las producciones tecnicas
	 * @return Lista de Producciones tecnicas
	 */
	public ArrayList<Produccion> extraerProdTecnica(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);

		Tipo tipo = new Tipo();

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {

			if (elem.get(i).contains("PRODUCCIÓN TÉCNICA")) {

				Produccion produccion = new Produccion();

				if (elem.get(i).contains("SOFTWARES")) {

					tipo = new Tipo(Constantes.ID_SOFTWARE, Constantes.SOFTWARE, tipoProduccion);

				} else if (elem.get(i).contains("PROTOTIPO")) {

					tipo = new Tipo(Constantes.ID_PROTOTIPO, Constantes.PROTOTIPO, tipoProduccion);

				} else if (elem.get(i).contains("PRODUCTOS TECNOLÓGICOS")) {

					tipo = new Tipo(Constantes.ID_PRODUCTO_TECNOLOGICO, Constantes.PRODUCTO_TECNOLOGICO,
							tipoProduccion);

				} else if (elem.get(i).contains("INNOVACIÓN")) {

					tipo = new Tipo(Constantes.ID_INNOVACION_PROCESO, Constantes.INNOVACION_PROCESO, tipoProduccion);

				} else if (elem.get(i).contains("CONSULTORÍA CIENTÍFICO TECNOLÓGICA E INFORME TÉCNICO")) {

					tipo = new Tipo(Constantes.ID_TRABAJO_TECNICO, Constantes.TRABAJO_TECNICO, tipoProduccion);

				} else if (elem.get(i).contains("REGULACIÓN, NORMA")) {

					tipo = new Tipo(Constantes.ID_NORMA, Constantes.NORMA, tipoProduccion);

				} else if (elem.get(i).contains("INFORMES DE INV")) {

					tipo = new Tipo(Constantes.ID_INFORME_INVESTIGACION, Constantes.INFORME_INVESTIGACION,
							tipoProduccion);

				}
				// Autores
				String general = elem.get(i + 1).substring(0, elem.get(i + 1).length() - 1);
				autores = general.substring(0, general.lastIndexOf(",")).replaceAll(", ", ",");
				String autoresFinal = verificarAutores(autores);

				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).startsWith("PRODUCCIÓN TÉCNICA")
						&& !elem.get(cont).startsWith("PALABRAS:") && !elem.get(cont).startsWith("AREAS:")
						&& !elem.get(cont).startsWith("SECTORES:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("EN: ")) {
						int pos = actual.lastIndexOf(",");
						anio = actual.substring(pos - 4, pos);
					}
					cont++;
				}
				referencia = referencia.substring(1);

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setReferencia(referencia);
				produccion.setAutores(autoresFinal);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae las empresas que poseen base tecnologica relacionadas con
	 * el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene las empresas que poseen base
	 *            tecnologica
	 * @return Lista de Producciones tecnicas
	 */
	public ArrayList<Produccion> extraerEmpresas(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_TECNICA, Constantes.TECNICA);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("EMPRESA DE BASE TECNOLÓGICA")) {

				Produccion produccion = new Produccion();

				// Autores
				String general = elem.get(i + 1).substring(0, elem.get(i + 1).length() - 1);
				autores = general.substring(0, general.lastIndexOf(",")).replaceAll(", ", ",");
				String autoresFinal = verificarAutores(autores);

				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("PRODUCCIÓN TÉCNICA")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("AREAS:")
						&& !elem.get(cont).contains("SECTORES:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("EL:")) {
						anio = elem.get(cont + 1).substring(0, 4);
					}
					cont++;
				}
				referencia = referencia.substring(1);

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccion.setReferencia(referencia);
				produccion.setAutores(autoresFinal);
				Tipo tipo = new Tipo(Constantes.ID_EMPRESA_TECNOLOGICA, Constantes.EMPRESA_TECNOLOGICA, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/*
	 * Extraer mas informacion
	 */

	/**
	 * Metodo que extrae los Demas trabajos publicados por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los Demas trabajos publicados por
	 *            el investigador
	 * @return Lista de Otros items
	 */
	public ArrayList<Produccion> extraerDemasTrabajos(ArrayList<String> elem, Investigador investigador) {

		String autores = "";
		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_MASINFORMACION, Constantes.MASINFORMACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("DEMÁS TRABAJOS -")) {
				Produccion produccion = new Produccion();

				// Autores
				String general = elem.get(i + 1).substring(0, elem.get(i + 1).length() - 1);
				autores = general.substring(0, general.lastIndexOf(",")).replaceAll(", ", ",");
				String autoresFinal = verificarAutores(autores);

				int cont = i + 1;
				referencia = "";
				while (cont < elem.size() && !elem.get(cont).contains("DEMÁS TRABAJOS -")
						&& !elem.get(cont).contains("PALABRAS:") && !elem.get(cont).contains("AREAS:")
						&& !elem.get(cont).contains("SECTORES:")) {
					String actual = elem.get(cont);
					referencia = referencia + " " + actual;
					if (actual.contains("EN: ")) {
						int pos = actual.lastIndexOf(",");
						anio = actual.substring(pos - 4, pos);
					}
					cont++;
				}
				referencia = referencia.trim();

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}
				produccion.setAutores(autoresFinal);
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_DEMAS_TRABAJOS, Constantes.DEMAS_TRABAJOS, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * Metodo que extrae los Proyectos publicados por el investigador
	 * 
	 * @param elem,
	 *            Lista de elementos que contiene los Proyectos publicados por el
	 *            investigador
	 * @return Lista de Otros items
	 */
	public ArrayList<Produccion> extraerProyectos(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_MASINFORMACION, Constantes.MASINFORMACION);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("TIPO DE PROYECTO:")) {
				Produccion produccion = new Produccion();

				referencia = elem.get(i);
				int cont = i + 1;
				while (cont < elem.size() && !elem.get(cont).contains("TIPO DE PROYECTO:")
						&& !elem.get(cont).contains("RESUMEN")) {
					String actual = elem.get(cont);
					referencia += " " + actual;
					if (actual.contains("DURACIÓN")) {
						anio = elem.get(cont - 1);
						anio = anio.substring(anio.length() - 4, anio.length());
					}
					cont++;
				}
				referencia = referencia.trim();

				if (!StringUtils.isNumeric(anio)) {
					anio = "N/D";
				}

				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_PROYECTO, Constantes.PROYECTO, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/*
	 * Extraer Producciones en Arte
	 */

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerObras(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("NOMBRE DEL PRODUCTO:")) {
				Produccion produccion = new Produccion();

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
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_OBRA, Constantes.OBRA, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerRegistrosAcuerdo(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("NOMBRE DEL PRODUCTO:")) {
				Produccion produccion = new Produccion();

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
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_ACUERDO_LICENCIA, Constantes.ACUERDO_LICENCIA, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerIndustrias(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("NOMBRE DE LA EMPRESA CREATIVA:")) {
				Produccion produccion = new Produccion();

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
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_INDUSTRIA_CREATIVA, Constantes.INDUSTRIA_CREATIVA, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return (produccionAux);
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerEventoArtistico(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("NOMBRE DEL EVENTO:")) {
				Produccion produccion = new Produccion();

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
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_EVENTO_ARTISTICO, Constantes.EVENTO_ARTISTICO, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return produccionAux;
	}

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public ArrayList<Produccion> extraerTallerCreativo(ArrayList<String> elem, Investigador investigador) {

		String referencia = "";
		String anio = "";

		TipoProduccion tipoProduccion = new TipoProduccion(Constantes.ID_ARTE, Constantes.ARTE);

		ArrayList<Produccion> produccionAux = new ArrayList<>();

		for (int i = 0; i < elem.size(); i++) {
			if (elem.get(i).contains("NOMBRE DEL TALLER:")) {
				Produccion produccion = new Produccion();

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
				produccion.setReferencia(referencia);
				Tipo tipo = new Tipo(Constantes.ID_TALLER_CREATIVO, Constantes.TALLER_CREATIVO, tipoProduccion);
				produccion.setTipo(tipo);
				produccion.setAnio(anio);
				produccion.setInvestigador(investigador);
				produccion.setRepetido("NO");
				identificarRepetidos(produccionAux, produccion);
				produccionAux.add(produccion);
			}
		}
		return produccionAux;
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

	/**
	 * 
	 * @param elem
	 * @return
	 */
	public void identificarRepetidosBibliograficos(ArrayList<ProduccionBibliografica> elem,
			ProduccionBibliografica produccionBibliografica) {
		String referenciaAuxInterno = produccionBibliografica.getReferencia();
		String anioAuxInterno = produccionBibliografica.getAnio();
		String idAuxInterno = produccionBibliografica.getIdentificador();
		for (int j = 0; j < elem.size(); j++) {
			String referenciaAux = elem.get(j).getReferencia();
			String anioAux = elem.get(j).getAnio();
			String idAux = elem.get(j).getIdentificador();
			if (referenciaAux.contains(referenciaAuxInterno) || referenciaAuxInterno.contains(referenciaAux)) {
				if (anioAux.contains(anioAuxInterno) || anioAuxInterno.contains(anioAux)) {
					if (idAux.contains(idAuxInterno) || idAuxInterno.contains(idAux)) {
						produccionBibliografica.setRepetido("SI");
					}
				}
			}
		}
	}
}