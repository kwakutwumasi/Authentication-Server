package com.quakearts.auth.server.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(info=@Info(title="QuakeArts Authentication Server", 
	description="The Authentication Server provides centralized authentication and authorization for API servers and micro-services. It is a simplified JWT token generator.\r\n" + 
			"\r\n" + 
			"The Authentication server provides API's for applications to register authentication and authorization mechanisms. It also provides facilities for hiding sensitive configuration parameters such as resource passwords and cryptographic keys. It also provides facilities for registering SQL data sources as Java Naming and Directory Interface (JNDI) data sources for use during authentication.\r\n" + 
			"\r\n" + 
			"Applications register authentication and authorization mechanisms by calling the \"Register an application for authentication\" operation. Each registration has an alias that applications can use when referring to it. The registration ID acts as a credential with which the registration details may be updated or removed.\r\n" + 
			"\r\n" + 
			"Applications may have more than one authentication and authorization mechanisms. These are referred to as Authentication Applications.\r\n" + 
			"\r\n" + 
			"Authentication Application configuration is based on Java Authentication and Authorization Services (JAAS).\r\n" + 
			"Each application is analogous to a Login Module. See JAAS documentation for more details. Available login modules depend on each deployment. To use a module, the module JAR and other dependencies must be located on the classpath.\r\n" + 
			"\r\n" + 
			"To authenticate and authorize a client, an alias and an Authentication Application name must be supplied along with the credentials. See the \"Generate a JWT token for authenticating to a server\" operation for more details.",
	license=@License(name="MIT",url="https://opensource.org/licenses/MIT"),
	contact=@Contact(email="info@quakearts.com", name="System Administration"),
	version="0.0.1"))
public class OpenApiDefinition {}
