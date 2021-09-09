package com.stream;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SupplierApplication {
	private BlockingQueue<Flux<String>> unbounded = new LinkedBlockingQueue<>();
	
	public static void main(String[] args) {
		SpringApplication.run(SupplierApplication.class, args);
	}
	
	@Bean
	public RouterFunction<ServerResponse> routes() {
		return RouterFunctions.route(RequestPredicates.GET("/greet/{msg}/"), (req) -> {
			unbounded.offer(Flux.range(0, 10).map(String::valueOf).delayElements(Duration.ofMillis(200)));
			
			return (Mono<ServerResponse>) 
					ServerResponse.ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(Mono.just(req.pathVariable("msg")), String.class);
		});
	}
    
    @PollableBean
    public Supplier<Flux<String>> msg() {
    	return () -> {
    		Flux<String> fluxMsg = unbounded.poll();
    		
    		return fluxMsg == null ? Flux.empty() : fluxMsg.switchIfEmpty(Flux.empty());
    	};
    }
}

