package com.example.springsoap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.xml.security.Init;

import com.example.springsoap.security.ValidationCallbackHandler;


@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {


    @Autowired
    private ValidationCallbackHandler validationCallbackHandler;

    @PostConstruct
    public void initXmlSecurity() {
        Init.init();  // Required by Apache Santuario (WSS4J)
    }

    @Bean
    public Wss4jSecurityInterceptor securityInterceptor() {
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor();
        interceptor.setValidationActions("UsernameToken");
        interceptor.setValidationCallbackHandler(validationCallbackHandler);
        return interceptor;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(securityInterceptor()); // Add WS-Security interceptor
    }

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);

        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "rooms")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema roomsSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("RoomsPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://foodmenu.io/gt/webservice");
        wsdl11Definition.setSchema(roomsSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema roomsSchema() {
        return new SimpleXsdSchema(new ClassPathResource("rooms.xsd"));
    }
}
