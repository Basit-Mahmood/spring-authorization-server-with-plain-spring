package pk.training.basit.configuration.context;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import pk.training.basit.controller.rest.RestControllerMarker;


@Configuration
@EnableWebMvc
@ComponentScan(
	basePackageClasses = {RestControllerMarker.class},
    useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter({ RestController.class, RestControllerAdvice.class })
)
public class RestServletContextConfiguration implements WebMvcConfigurer {
	
	private static final Logger log = LogManager.getLogger();

	 @Autowired
	 private ApplicationContext applicationContext;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private Marshaller marshaller;
	
	@Autowired
	private Unmarshaller unmarshaller;
	
	@Autowired
	private SpringValidatorAdapter validator;

	/**
	 * 
	 * 		<mvc:annotation-driven content-negotiation-manager="contentManager" >
   
        		<mvc:message-converters>
        		
        			<bean class="org.springframework.http.converter.xml.MarshallingHttpMessageConverter"
                		p:marshaller-ref="webProxyJaxbMarshaller"
                		p:unmarshaller-ref="webProxyJaxbMarshaller"/>
                		
            		<bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"
                		p:supportedMediaTypes="application/json" 
            			p:objectMapper-ref="jacksonMapperFactory" />
           
        		</mvc:message-converters>
    		</mvc:annotation-driven>
	 */
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		
		converters.add(new SourceHttpMessageConverter<>());

		MarshallingHttpMessageConverter xmlConverter = new MarshallingHttpMessageConverter();
		xmlConverter.setSupportedMediaTypes(Arrays.asList(
				new MediaType("application", "xml"), 
				new MediaType("text", "xml")
		));
		xmlConverter.setMarshaller(this.marshaller);
		xmlConverter.setUnmarshaller(this.unmarshaller);
		converters.add(xmlConverter);

		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
		jsonConverter.setSupportedMediaTypes(Arrays.asList(
				new MediaType("application", "json"), 
				new MediaType("text", "json")
		));
		jsonConverter.setObjectMapper(this.objectMapper);
		converters.add(jsonConverter);
	}

	/**
	 * 		<bean id="contentManager" class="org.springframework.web.accept.ContentNegotiationManagerFactoryBean"
				p:favorPathExtension="false"
				p:favorParameter="false" 
	    		p:ignoreAcceptHeader="false" 
	    		p:defaultContentType="application/json" />
	    		
	    	<util:map id="mediaTypesMap">
    			<entry key="json" value="application/json" />
	 			<entry key="xml" value="application/xml" />
			</util:map>
	 * 
	 */
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		
		configurer
				.favorParameter(false)
				.ignoreAcceptHeader(false)
				.defaultContentType(MediaType.APPLICATION_JSON);
		
	}

	@Override
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * 	
			<beans:bean id="localeResolver" class="org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver"  
        		p:cookieName="locale" />
	 * 
	 * @return
	 */
	@Bean
	public LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver();
	}
	
	@Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        
		Integer page = 0;
		Integer size = 20;
		
		Sort defaultSort = Sort.by(Sort.Direction.ASC, "id");
        Pageable defaultPageable = PageRequest.of(page, size, defaultSort);
		
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        sortResolver.setSortParameter("$paging.sort");
        sortResolver.setFallbackSort(defaultSort);

        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver(sortResolver);
        pageableResolver.setMaxPageSize(200);
        pageableResolver.setOneIndexedParameters(true);
        pageableResolver.setPrefix("$paging.");
        pageableResolver.setFallbackPageable(defaultPageable);

        resolvers.add(sortResolver);
        resolvers.add(pageableResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        
    	if(!(registry instanceof FormattingConversionService)) {
            log.warn("Unable to register Spring Data JPA converter.");
            return;
        }

        DomainClassConverter<FormattingConversionService> converter = new DomainClassConverter<>((FormattingConversionService)registry);
        converter.setApplicationContext(this.applicationContext);
    }
	
}
