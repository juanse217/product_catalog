package com.sebastian.dev.productcatalog.controller.advice;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.sebastian.dev.productcatalog.exception.ProductNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex){
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        p.setTitle("Bad request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(p);
    }

    @ExceptionHandler(value = ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ProblemDetail> handleProductNotFoundException(ProductNotFoundException ex){
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setTitle("Product not found");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(p);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request){
        
        ProblemDetail p = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        p.setTitle("Invalid request argument");
        p.setProperty("errors", ex.getBindingResult().getFieldErrors());
        p.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(p);
    }
}
