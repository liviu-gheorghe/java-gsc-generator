package com.liviugheorghe.gettersandsettersgenerator;

import com.liviugheorghe.gettersandsettersgenerator.java.Generator;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {

        String filename = args[1];
        Generator generator = new Generator(filename);
        generator.generate(Generator.GenerationStrategy.SAME_FILE);
    }
}