package com.mycorp.bo;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mycorp.assembler.ZendeskAssembler;
import com.mycorp.rest.Zendesk;
import com.mycorp.support.CorreoElectronico;
import com.mycorp.support.DatosCliente;
import com.mycorp.support.MensajeriaService;
import com.mycorp.support.Poliza;
import com.mycorp.support.PolizaBasicoFromPolizaBuilder;
import com.mycorp.support.Ticket;
import com.mycorp.support.ValueCode;
import com.mycorp.utils.Constantes;

import portalclientesweb.ejb.interfaces.PortalClientesWebEJBRemote;
import util.datos.PolizaBasico;
import util.datos.UsuarioAlta;

public class ZendeskBo {

	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger( ZendeskBo.class );

	@Value("#{envPC['zendesk.ticket']}")
	public String PETICION_ZENDESK= "";

	@Value("#{envPC['zendesk.token']}")
	public String TOKEN_ZENDESK= "";

	@Value("#{envPC['zendesk.url']}")
	public String URL_ZENDESK= "";

	@Value("#{envPC['zendesk.user']}")
	public String ZENDESK_USER= "";

	@Value("#{envPC['tarjetas.getDatos']}")
	public String TARJETAS_GETDATOS = "";

	@Value("#{envPC['cliente.getDatos']}")
	public String CLIENTE_GETDATOS = "";

	@Value("#{envPC['zendesk.error.mail.funcionalidad']}")
	public String ZENDESK_ERROR_MAIL_FUNCIONALIDAD = "";

	@Value("#{envPC['zendesk.error.destinatario']}")
	public String ZENDESK_ERROR_DESTINATARIO = "";

	/** The portalclientes web ejb remote. */
	@Autowired
	// @Qualifier("portalclientesWebEJB")
	private PortalClientesWebEJBRemote portalclientesWebEJBRemote;

	/** The rest template. */
	@Autowired
	@Qualifier("restTemplateUTF8")
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier( "emailService" )
	MensajeriaService emailService;

	@Autowired
	ZendeskAssembler zendeskAssembler;

	/**
	 * Crea un ticket en Zendesk. Si se ha informado el nÂº de tarjeta, obtiene los datos asociados a dicha tarjeta de un servicio externo.
	 * @param usuarioAlta
	 * @param userAgent
	 * @throws Exception 
	 */
	public String altaTicketZenDesk(UsuarioAlta usuarioAlta, String userAgent) throws Exception{

		LOG.trace("Entra en altaTicketZenDesk del bo");

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		StringBuilder datosUsuario = new StringBuilder();
		StringBuilder clientName = new StringBuilder();
		String idCliente = null;
		StringBuilder datosServicio = new StringBuilder();
		StringBuilder datosBravo = new StringBuilder();

		try {

			//Obtenemos los datos del formulario a traves de la entidad UsuarioAlta y el campo userAgent
			datosUsuario = zendeskAssembler.getDatosFormulario(usuarioAlta, userAgent);

			//Obtenemos los datos de la tarjeta o de la poliza
			obtenerDatosTarjetaPoliza(usuarioAlta, clientName, idCliente, datosServicio, mapper);

			//Obtenemos los datos del cliente
			DatosCliente cliente = restTemplate.getForObject("http://localhost:8080/test-endpoint", DatosCliente.class, idCliente);

			//Obtenemos los tipos de documentos
			List< ValueCode > tiposDocumentos = getTiposDocumentosRegistro();

			//Obtenemos los datos bravo a traves de los tipos de documentos y los datos de cliente
			datosBravo = zendeskAssembler.getDatosBravo(tiposDocumentos, cliente);

			//Formateamos los datos anteriormente recogidos para formar el ticket
			String ticket = String.format(PETICION_ZENDESK, clientName.toString(), usuarioAlta.getEmail(), datosUsuario.toString()+datosBravo.toString()+parseJsonBravo(datosServicio));
			ticket = ticket.replaceAll("["+Constantes.ESCAPED_LINE_SEPARATOR+"]", " ");

			//Se crea el ticket y en caso de haber error se envía un email al usuario indicándolo
			crearTicket(mapper, ticket, datosUsuario, datosBravo);

		} catch (Exception e) {
			LOG.error("Error al crear ticket ZENDESK", e.getCause());
			throw e;
		}

		datosUsuario.append(datosBravo);

		LOG.trace("Sale de altaTicketZenDesk del bo");
		return datosUsuario.toString();
	}

	public List< ValueCode > getTiposDocumentosRegistro() {
		return Arrays.asList( new ValueCode(), new ValueCode() ); // simulacion servicio externo
	}

	/**
	 * Método para parsear el JSON de respuesta de los servicios de tarjeta/pÃ³liza
	 *
	 * @param resBravo
	 * @return
	 */
	private String parseJsonBravo(StringBuilder resBravo)
	{
		return resBravo.toString().replaceAll("[\\[\\]\\{\\}\\\"\\r]", "").replaceAll(Constantes.ESCAPED_LINE_SEPARATOR, Constantes.ESCAPE_ER + Constantes.ESCAPED_LINE_SEPARATOR);
	}

	/**
	 * Método para obtener los datos de la tarjeta o póliza del usuario
	 *
	 * @param usuarioAlta
	 * @param clientName
	 * @param idCliente
	 * @param datosServicio
	 * @param mapper
	 * @return void
	 */
	public void obtenerDatosTarjetaPoliza (UsuarioAlta usuarioAlta, StringBuilder clientName, String idCliente, StringBuilder datosServicio, ObjectMapper mapper) throws Exception {
		LOG.trace("Entra en obtenerDatosTarjetaPoliza del bo");

		//Si el usuario tiene número de tarjeta se obtienen los datos del servicio rest de tarjetas
		if(StringUtils.isNotBlank(usuarioAlta.getNumTarjeta())){
			try{
				String urlToRead = TARJETAS_GETDATOS + usuarioAlta.getNumTarjeta();
				ResponseEntity<String> res = restTemplate.getForEntity( urlToRead, String.class);
				if(res.getStatusCode() == HttpStatus.OK){
					String dusuario = res.getBody();
					clientName.append(dusuario);
					idCliente = dusuario;
					datosServicio.append("Datos recuperados del servicio de tarjeta:").append(Constantes.ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(dusuario));
				}
			}catch(Exception e)
			{
				LOG.error("Error al obtener los datos de la tarjeta", e);
				throw e;
			}
		}
		//Si el usuario no tiene número de tarjeta pero si de póliza se obtienen los datos del servicio rest de polizas
		else if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
			try
			{
				Poliza poliza = new Poliza(Integer.valueOf(usuarioAlta.getNumPoliza()), Integer.valueOf(usuarioAlta.getNumDocAcreditativo()), 1);

				PolizaBasico polizaBasicoConsulta = new PolizaBasicoFromPolizaBuilder().withPoliza( poliza ).build();

				final util.datos.DetallePoliza detallePolizaResponse = portalclientesWebEJBRemote.recuperarDatosPoliza(polizaBasicoConsulta);

				clientName.append(detallePolizaResponse.getTomador().getNombre()).
				append(" ").
				append(detallePolizaResponse.getTomador().getApellido1()).
				append(" ").
				append(detallePolizaResponse.getTomador().getApellido2());

				idCliente = detallePolizaResponse.getTomador().getIdentificador();
				datosServicio.append("Datos recuperados del servicio de tarjeta:").append(Constantes.ESCAPED_LINE_SEPARATOR).append(mapper.writeValueAsString(detallePolizaResponse));
			}catch(Exception e)
			{
				LOG.error("Error al obtener los datos de la poliza", e);
				throw e;
			}
		}
		LOG.trace("Sale de obtenerDatosTarjetaPoliza del bo");
	}

	/**
	 * Método para enviar un email
	 *
	 * @param correo
	 * @return void
	 */
	public void sendMail (CorreoElectronico correo) {
		LOG.trace("Entra en sendMail del bo");

		try {
			emailService.enviar( correo );
		} catch(Exception e) {
			LOG.error("Error al enviar mail", e);
			throw e;
		}
		LOG.trace("Sale de sendMail del bo");
	}

	public void crearTicket (ObjectMapper mapper, String ticket, StringBuilder datosUsuario, StringBuilder datosBravo) {
		LOG.trace("Entra en crearTicket del bo");

		try(Zendesk zendesk = new Zendesk.Builder(URL_ZENDESK).setUsername(ZENDESK_USER).setToken(TOKEN_ZENDESK).build()){
			//Ticket
			Ticket petiZendesk = mapper.readValue(ticket, Ticket.class);
			zendesk.createTicket(petiZendesk);
			LOG.trace("Se ha creado el ticket correctamente");
		}catch(Exception e){
			LOG.error("Error al crear ticket ZENDESK", e);
			//Si no se ha podido crear el ticket, se forma un correo para indicar que dicho ticket no se ha podido crear
			CorreoElectronico correo = new CorreoElectronico( Long.parseLong(ZENDESK_ERROR_MAIL_FUNCIONALIDAD), "es" )
					.addParam(datosUsuario.toString().replaceAll(Constantes.ESCAPE_ER+Constantes.ESCAPED_LINE_SEPARATOR, Constantes.HTML_BR))
					.addParam(datosBravo.toString().replaceAll(Constantes.ESCAPE_ER+Constantes.ESCAPED_LINE_SEPARATOR, Constantes.HTML_BR));
			correo.setEmailA( ZENDESK_ERROR_DESTINATARIO );

			//Se envía el correo
			sendMail(correo);
		}
		LOG.trace("Sale de crearTicket del bo");
	}
}