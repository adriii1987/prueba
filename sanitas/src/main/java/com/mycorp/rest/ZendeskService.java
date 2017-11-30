package com.mycorp.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.mycorp.bo.ZendeskBo;
import com.mycorp.support.MensajeriaService;
import com.mycorp.utils.ZendeskException;

import util.datos.UsuarioAlta;

@Service
public class ZendeskService {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger( ZendeskService.class );

    /** The rest template. */
    @Autowired
    @Qualifier("restTemplateUTF8")
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier( "emailService" )
    MensajeriaService emailService;

    @Autowired
    ZendeskBo zenDeskBo;
    
    /**
     * Crea un ticket en Zendesk. Si se ha informado el nÂº de tarjeta, obtiene los datos asociados a dicha tarjeta de un servicio externo.
     * @param usuarioAlta
     * @param userAgent
     */
    public String altaTicketZendesk(UsuarioAlta usuarioAlta, String userAgent){

    	LOG.trace("Entra en el servicio altaTicketZendesk");
    	String altaTicket = "";
    	try {
    		//Llamamos al bo de lata ticket
    		altaTicket = zenDeskBo.altaTicketZenDesk(usuarioAlta, userAgent);
    	} catch (Exception e){
    		throw new ZendeskException("Se ha producido un error al crear el ticket en Zendesk", e.getCause());
    	}
    	LOG.trace("Sale del servicio altaTicketZendesk");
    	return altaTicket;
    }
}