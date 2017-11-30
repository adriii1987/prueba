package com.mycorp.assembler;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mycorp.support.DatosCliente;
import com.mycorp.support.ValueCode;
import com.mycorp.utils.Constantes;

import util.datos.UsuarioAlta;

public class ZendeskAssembler {

	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger( ZendeskAssembler.class );

	private SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

	public StringBuilder getDatosFormulario(UsuarioAlta usuarioAlta, String userAgent) {
		LOG.trace("Entra en getDatosFormulario del assembler");

		StringBuilder datosUsuario = new StringBuilder();
		try {
			// Añade los datos del formulario
			if(StringUtils.isNotBlank(usuarioAlta.getNumPoliza())){
				datosUsuario.append("Nº de poliza/colectivo: ").append(usuarioAlta.getNumPoliza()).append("/").append(usuarioAlta.getNumDocAcreditativo()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			}else{
				datosUsuario.append("Nº tarjeta Sanitas o Identificador: ").append(usuarioAlta.getNumTarjeta()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			}
			datosUsuario.append("Tipo documento: ").append(usuarioAlta.getTipoDocAcreditativo()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			datosUsuario.append("Nº documento: ").append(usuarioAlta.getNumDocAcreditativo()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			datosUsuario.append("Email personal: ").append(usuarioAlta.getEmail()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			datosUsuario.append("Nº móvil: ").append(usuarioAlta.getNumeroTelefono()).append(Constantes.ESCAPED_LINE_SEPARATOR);
			datosUsuario.append("User Agent: ").append(userAgent).append(Constantes.ESCAPED_LINE_SEPARATOR);
		} catch (Exception e){
			throw e;
		}
		LOG.trace("Sale de getDatosFormulario del assembler");
		return datosUsuario;
	}

	public StringBuilder getDatosBravo(List< ValueCode > tiposDocumentos, DatosCliente cliente) throws Exception {
		LOG.trace("Entra en getDatosBravo del assembler");

		StringBuilder datosBravo = new StringBuilder();

		try
		{           
			datosBravo.append(Constantes.ESCAPED_LINE_SEPARATOR + "Datos recuperados de BRAVO:" + Constantes.ESCAPED_LINE_SEPARATOR + Constantes.ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Teléfono: ").append(cliente.getGenTGrupoTmk()).append(Constantes.ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Feha de nacimiento: ").append(formatter.format(formatter.parse(cliente.getFechaNacimiento()))).append(Constantes.ESCAPED_LINE_SEPARATOR);

			for(int i = 0; i < tiposDocumentos.size();i++)
			{
				if(tiposDocumentos.get(i).getCode().equals(cliente.getGenCTipoDocumento().toString()))
				{
					datosBravo.append("Tipo de documento: ").append(tiposDocumentos.get(i).getValue()).append(Constantes.ESCAPED_LINE_SEPARATOR);
				}
			}
			datosBravo.append("Número documento: ").append(cliente.getNumeroDocAcred()).append(Constantes.ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Tipo cliente: ");
			switch (cliente.getGenTTipoCliente()) {
			case 1:
				datosBravo.append("POTENCIAL").append(Constantes.ESCAPED_LINE_SEPARATOR);
				break;
			case 2:
				datosBravo.append("REAL").append(Constantes.ESCAPED_LINE_SEPARATOR);
				break;
			case 3:
				datosBravo.append("PROSPECTO").append(Constantes.ESCAPED_LINE_SEPARATOR);
				break;
			}

			datosBravo.append("ID estado del cliente: ").append(cliente.getGenTStatus()).append(Constantes.ESCAPED_LINE_SEPARATOR);

			datosBravo.append("ID motivo de alta cliente: ").append(cliente.getIdMotivoAlta()).append(Constantes.ESCAPED_LINE_SEPARATOR);

			datosBravo.append("Registrado: ").append((cliente.getfInactivoWeb() == null ? "Sí" : "No")).append(Constantes.ESCAPED_LINE_SEPARATOR + Constantes.ESCAPED_LINE_SEPARATOR);

		}catch (Exception e){
			throw e;
		}
		LOG.trace("Entra en getDatosBravo del assembler");
		return datosBravo;
	}
}
