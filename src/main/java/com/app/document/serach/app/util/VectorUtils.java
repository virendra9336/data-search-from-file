package com.app.document.serach.app.util;

import java.util.List;

public class VectorUtils {
    public static double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot=0, na=0, nb=0;
        for (int i=0;i<a.size();i++){
            double va=a.get(i), vb=b.get(i);
            dot += va*vb;
            na += va*va; nb += vb*vb;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10);
    }
}
