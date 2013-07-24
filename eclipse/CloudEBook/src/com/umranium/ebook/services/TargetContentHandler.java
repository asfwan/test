package com.umranium.ebook.services;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

public interface TargetContentHandler {
    public void handle(String target, Request request, HttpServletRequest mainRequestObject,
                       HttpServletResponse response) throws IOException, ServletException;
}
