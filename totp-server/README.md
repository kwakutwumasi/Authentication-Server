# TOTP-Server
A Server implementing time based one time PINs (TOTP's) as described in [RFC 6238](https://tools.ietf.org/html/rfc6238). It can be integrated into Authentication Server with some minor adjustments, or deployed as a standalone service using QA-Appbase or any fully certified JEE 7 server.

### Operation

The server has a REST interface for management and provisioning. The management interface allows administrators to 
* create device aliases,
* lock and unlock devices, 
* create additional administrator devices, and
* deactivate devices

### Devices

A device is defined as any software or hardware capable of generating tokens using the [RFC 6238](https://tools.ietf.org/html/rfc6238) algorithm. The HMAC algorithm and expected length must be set to the same values, or else tokens will not match. An example Android Implementation is provided in a separate project.

Devices must be provisioned before usage. Provisioning involves the generation of a cryptographic seed and the initial counter. These are transmitted to the device for secure storage over a secure communication channel.

Once provisioned, a device must be activated by sending a generated token to the server. The server validates the token and sets the device to active. The device can then be authenticated by the server with a new time based token.

The devices provide their own ID's during provisioning. ID's are checked for uniqueness, and as such it is important to use unique identifiers. An example of a unique identifier would be the Android Device ID or the IMIE number (for smartphones). Device creators should take this into account when programming their device for provisioning. The sample application uses the Android Device ID as the unique identifier.

### Aliases

Aliases can be used to associate an application specific identifier to a device. 
As an example a bank may want its customers to log on to an Internet Banking platform using a bank account numbers. If the device ID in use is not the same as this account number, clients would have to enter the device id during login. The account number can be setup on the TOTP server as an alias for the customer specific device. During authentication, the account number can be used in place of the device id. The server will look for the device record using the alias.

Multiple aliases can be assigned to a single device. 

Aliases must be unique.

### The TOTPLoginModule

The TOTPLoginModule is a Java Authentication and Authorization Service (JAAS) module. It can be used by any system that supports the usage of JAAS login modules. The Authentication Server is one such service.

The module has no required setup parameters. 

Optional parameters are 

```
totp.rolename - This parameter is for use in JAAS modules that are 
 integrated with Jboss/Wildfly's authentication system. All roles are 
 grouped within a java.security.acl.Group with a specific name. This 
 defaults to Roles

use_first_pass - A string that evaluates to the Boolean value of true or 
 false. If true, and multiple modules are used, it will attempt to use the 
 username and password from the previous module, if any, rather than 
 perform a callback
 
totp.defaultroles - A comma separated list of the default roles to 
 assign to all authenticated subjects
 
```

### Encryption

The server uses encryption to store seeds and other security values in a database. See QA-Crypto for more information on how to setup encryption.

For the highest level of security, a Hardware Security Module (HSM) should be used. Any HSM that provides a Java Cryptography Extension (JCE) module can be used with QA-Crypto. A HSM will protect the encryption keys from being pulled off the server, ensuring that only authorized applications on the server can encrypt and decrypt secured information.

### Security Features

Aside from encryption the server has some security features to prevent tampering.

To protect against device ID switching, an encrypted check value is stored in the database. This check value is verified each time the device is retrieved. If an attacker manages to gain access to the database and tries to switch their device ID with that of a target, they would need to generate and encrypt the check value. If the encryption key is stored securely, this will not be feasible.

The same protection exists for aliases and administrator accounts. This 

### Setup and Configuration

###### Using Authentication Server

The libraries for the TOTP server must be included in the Authentication Server class path. Since Authentication Server uses the QA-Appbase, the basic platform for running the login module is in place. Authentication Server uses JAX-RS to provide its interface. The web server within Authentication Server will automatically load the REST interfaces for TOTP. 

The server will require an implementation of QA-ORM, specifically a Java Persistent Architecture compatible implementation. Non JPA implementations will work. However they must support transparent encryption and decryption of the secured fields, using QA-Crypto's _com.quakearts.security.cryptography.jpa.EncryptedValueConverter_ and _com.quakearts.security.cryptography.jpa.EncryptedValueStringConverter_. 

The reference implementation uses QA-ORM-Hibernate as its ORM implementation. QA-ORM-Hibernate requires a Java Connectivity Architecure (JCA) datasource. Since Authentication server excludes these libraries, they would need to be explicitly added. The Java Transacton Architecture (JTA) libraries can also be added to simplify transaction management.

Two other files need to be on the classpath:
1. login.config - this is a JAAS login configuration for authenticating tokens used to access the management interface. It must be configured with an appropriate _com.quakearts.webapp.security.auth.JWTLoginModule_ named 'TOTP-JWT-Login'. See QA-Auth for more information on configuring _JWTLoginModule_.

2. totpoptions.json - This file contains important setup information for the TOTP server:

```
data.store.name - The name of the QA-ORM datastore for the TOTP server
mac.algorithm - The specific HMAC algorithm to use to generate the tokens 
mac.provider - The JCE provider of the MAC generation algorithm
otp.length - the OTP string length
seed.length - the length of the seed to generate, in bytes. It must be appropriate for the selected MAC algorithm
secure.random.instance - The secure random instance to use when generating the seed
secure.random.provider - the JCE provider of the secure random instance
time.step - the amount of time in seconds that the token is valid for
grace.period - the amount time in seconds to consider tokens generated in the previous time step. This is to allow for network latency and un-syncronized clocks
max.attempts - the maximum number of tries before a device is locked
lockout.time - the amount of time, in microseconds before the lockout tries are reset. If a device has been locked, the reset will have no effect.
installed.administrators - the initial list of device administrators. These must be setup prior to server initiation and prior to provisioning of the devices.
count.query - The SQL server query to use to when pulling the total device count (Only necessary for SQL server based QA-ORM implementations)

```

###### Setup Notes

Prior to the first server initialization, a minimum of two (2) devices must be selected as administrators. One device is used to create records. The other is used to approve the record creation. 

The selected devices will need to generate device ID's prior to provisioning. These device ID's are configured in the applications setup file _totpoptions.json_ in the 'installed.administrators' section of the configuration. Below is an example of the entries: 

```
	"installed.administrators":{
		"map":{"testadministrator1":"Adminstrator 1","testadministrator2":"Adminstrator 2"}
	}
```

The map key is the device ID and the value is the name given to that device. It should be descriptive such as "Kofi Babone's iPhone 6S" or "John Smith's Samsung Galaxy 5" to make it easy for administrators to know the owner and the specific device.

Once this and other parameters have been set, the server can be started and the administrator device provisioned. The devices will be added to the set of administrator devices during provisioning.

It is important to follow these steps in order. Failing to do so, the TOTP server will not have any administrator devices, and any modification to the system through the Management interface will be impossible.

### The Authentication Algorithm

See [RFC 6238](https://tools.ietf.org/html/rfc6238) for more details on how the algorithm works.