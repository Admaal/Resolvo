package com.admaal.resolvo.application;

public interface VerificadorContrasena {
  boolean coincide(String plana, String hash);
}
