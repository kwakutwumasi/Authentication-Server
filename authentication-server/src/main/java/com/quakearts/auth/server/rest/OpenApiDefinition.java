package com.quakearts.auth.server.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(info=@Info(title="QuakeArts Authentication Server", 
	description="Central Authentication Server for Core API servers",
	license=@License(name="MIT",url="https://opensource.org/licenses/MIT"),
	contact=@Contact(email="info@quakearts.com", name="System Administration"),
	version="0.0.1"))
public class OpenApiDefinition {}
