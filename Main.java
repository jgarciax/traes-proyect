import java.util.*;

// Mejora: validación de entrada y control de errores implementado (por daniel)
public class Main {

    // Nodo del árbol
    static class Nodo {
        String valor;
        Nodo izquierdo, derecho;

        Nodo(String valor) {
            this.valor = valor;
        }
    }

    // Variables globales para el parser
    static String expresion;
    static int pos;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Evaluador de Expresiones Matemáticas   ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Operadores permitidos: + - * / ^ √");
        System.out.println("Ejemplo: a + b - (c - b) + e\n");

        // Leer expresión
        System.out.print("Ingresa la expresión: ");
        String entrada = sc.nextLine().trim();

        // Validar vacío
        if (entrada.isEmpty()) {
            System.out.println("❌ Error: La expresión no puede estar vacía.");
            sc.close();
            return;
        }

        // Validar caracteres
        if (!validarCaracteres(entrada)) {
            System.out.println("❌ Error: La expresión contiene caracteres no permitidos.");
            sc.close();
            return;
        }

        // Validar paréntesis
        if (!validarParentesis(entrada)) {
            System.out.println("❌ Error: Paréntesis desbalanceados.");
            sc.close();
            return;
        }

        // Detectar variables
        Set<Character> vars = detectarVariables(entrada);

        // Pedir valores
        Map<String, Double> valores = new HashMap<>();
        if (!vars.isEmpty()) {
            System.out.println("\n🔎 Variables detectadas. Ingresa sus valores:");
            int i = 1;
            for (char v : new TreeSet<>(vars)) {
                System.out.print("  " + i + ". " + v + " = ? ");
                while (!sc.hasNextDouble()) {
                    System.out.print("     Valor inválido, intenta de nuevo: ");
                    sc.next();
                }
                valores.put(String.valueOf(v), sc.nextDouble());
                i++;
            }
        }

        try {
            expresion = entrada.replaceAll("\\s+", "");
            pos = 0;

            Nodo raiz = parsearSumaResta();

            // Validar que toda la expresión fue usada
            if (pos < expresion.length()) {
                throw new RuntimeException("Error en la expresión cerca de: '" + expresion.charAt(pos) + "'");
            }

            System.out.println("\n📌 Árbol de expresión:");
            imprimirArbol(raiz, "", true);

            double resultado = evaluar(raiz, valores);
            System.out.println("\n✅ Resultado: " + resultado);

        } catch (Exception e) {
            System.out.println("❌ Error al procesar la expresión: " + e.getMessage());
        }

        sc.close();
    }

    // Validación de caracteres
    static boolean validarCaracteres(String expr) {
        return expr.matches("[a-zA-Z0-9+\\-*/^√().\\s]+");
    }

    // Validar paréntesis balanceados
    static boolean validarParentesis(String expr) {
        int contador = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') contador++;
            if (c == ')') contador--;
            if (contador < 0) return false;
        }
        return contador == 0;
    }

    // Detectar variables
    static Set<Character> detectarVariables(String expr) {
        Set<Character> vars = new LinkedHashSet<>();
        for (char c : expr.toCharArray()) {
            if (Character.isLetter(c)) {
                vars.add(c);
            }
        }
        return vars;
    }

    // PARSER RECURSIVO DESCENDENTE

    static Nodo parsearSumaResta() {
        Nodo nodo = parsearMultDiv();
        while (pos < expresion.length() &&
               (expresion.charAt(pos) == '+' || expresion.charAt(pos) == '-')) {

            char op = expresion.charAt(pos++);
            Nodo derecho = parsearMultDiv();

            Nodo padre = new Nodo(String.valueOf(op));
            padre.izquierdo = nodo;
            padre.derecho = derecho;

            nodo = padre;
        }
        return nodo;
    }

    static Nodo parsearMultDiv() {
        Nodo nodo = parsearPotencia();
        while (pos < expresion.length() &&
               (expresion.charAt(pos) == '*' || expresion.charAt(pos) == '/')) {

            char op = expresion.charAt(pos++);
            Nodo derecho = parsearPotencia();

            Nodo padre = new Nodo(String.valueOf(op));
            padre.izquierdo = nodo;
            padre.derecho = derecho;

            nodo = padre;
        }
        return nodo;
    }

    static Nodo parsearPotencia() {
        Nodo nodo = parsearUnario();

        if (pos < expresion.length() && expresion.charAt(pos) == '^') {
            pos++;
            Nodo derecho = parsearPotencia();

            Nodo padre = new Nodo("^");
            padre.izquierdo = nodo;
            padre.derecho = derecho;

            return padre;
        }

        return nodo;
    }

    static Nodo parsearUnario() {
        if (pos < expresion.length() && expresion.charAt(pos) == '√') {
            pos++;
            Nodo hijo = parsearUnario();

            Nodo nodo = new Nodo("√");
            nodo.derecho = hijo;
            return nodo;
        }

        if (pos < expresion.length() && expresion.charAt(pos) == '-') {
            pos++;
            Nodo hijo = parsearUnario();

            Nodo nodo = new Nodo("neg");
            nodo.derecho = hijo;
            return nodo;
        }

        return parsearPrimario();
    }

    static Nodo parsearPrimario() {
        if (pos >= expresion.length()) {
            throw new RuntimeException("Expresión incompleta.");
        }

        char c = expresion.charAt(pos);

        if (c == '(') {
            pos++;
            Nodo nodo = parsearSumaResta();

            if (pos >= expresion.length() || expresion.charAt(pos) != ')') {
                throw new RuntimeException("Falta ')'");
            }

            pos++;
            return nodo;
        }

        if (Character.isDigit(c) || c == '.') {
            StringBuilder sb = new StringBuilder();

            while (pos < expresion.length() &&
                   (Character.isDigit(expresion.charAt(pos)) || expresion.charAt(pos) == '.')) {

                sb.append(expresion.charAt(pos++));
            }

            return new Nodo(sb.toString());
        }

        if (Character.isLetter(c)) {
            pos++;
            return new Nodo(String.valueOf(c));
        }

        throw new RuntimeException("Carácter inválido: '" + c + "'");
    }

    // Evaluación
    static double evaluar(Nodo nodo, Map<String, Double> valores) {
        if (nodo == null) {
            throw new RuntimeException("Nodo nulo.");
        }

        String v = nodo.valor;

        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ignored) {}

        if (v.length() == 1 && Character.isLetter(v.charAt(0))) {
            if (!valores.containsKey(v)) {
                throw new RuntimeException("Variable '" + v + "' sin valor.");
            }
            return valores.get(v);
        }

        double izq = (nodo.izquierdo != null) ? evaluar(nodo.izquierdo, valores) : 0;
        double der = evaluar(nodo.derecho, valores);

        switch (v) {
            case "+": return izq + der;
            case "-": return izq - der;
            case "*": return izq * der;
            case "/":
                if (der == 0) throw new RuntimeException("División por cero.");
                return izq / der;
            case "^": return Math.pow(izq, der);
            case "√": return Math.sqrt(der);
            case "neg": return -der;
            default: throw new RuntimeException("Operador desconocido: " + v);
        }
    }

    // Imprimir árbol
    static void imprimirArbol(Nodo nodo, String prefijo, boolean esUltimo) {
        if (nodo == null) return;

        System.out.println(prefijo + (esUltimo ? "└── " : "├── ") + nodo.valor);

        String nuevoPrefijo = prefijo + (esUltimo ? "    " : "│   ");

        if (nodo.izquierdo != null || nodo.derecho != null) {
            if (nodo.izquierdo != null)
                imprimirArbol(nodo.izquierdo, nuevoPrefijo, nodo.derecho == null);

            if (nodo.derecho != null)
                imprimirArbol(nodo.derecho, nuevoPrefijo, true);
        }
    }
}