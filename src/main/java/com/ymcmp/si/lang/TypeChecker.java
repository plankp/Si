/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package com.ymcmp.si.lang;

import static com.ymcmp.si.lang.LiteralUtils.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ymcmp.midform.tac.value.*;
import com.ymcmp.midform.tac.type.*;

import com.ymcmp.si.lang.grammar.SiBaseVisitor;
import com.ymcmp.si.lang.grammar.SiLexer;
import com.ymcmp.si.lang.grammar.SiParser;
import com.ymcmp.si.lang.type.*;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public final class TypeChecker extends SiBaseVisitor<Object> {

    private final Map<Path, SiParser.FileContext> importMap = new LinkedHashMap<>();

    private final LinkedList<InstantiatedFunction.Local> ifuncQueue = new LinkedList<>();

    private String namespacePrefix;
    private Path currentFile;
    private boolean isExported;

    public TypeChecker() {
        this.reset();
    }

    public void reset() {
        this.importMap.clear();

        this.ifuncQueue.clear();

        this.namespacePrefix = "";
        this.currentFile = null;
        this.isExported = false;
    }

    public boolean loadSource(final String raw) {
        return this.loadSource(Paths.get(raw));
    }

    public boolean loadSource(final Path rawPath) {
        final Path path = rawPath.normalize().toAbsolutePath();
        if (this.importMap.containsKey(path)) {
            return false;
        }

        final Path saved = this.currentFile;

        try {
            final SiLexer lexer = new SiLexer(CharStreams.fromPath(path));
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final SiParser parser = new SiParser(tokens);

            final SiParser.FileContext ctx = parser.file();

            this.importMap.put(path, ctx);
            this.currentFile = path;
            this.visitFile(ctx);

            this.importMap.remove(path);
            this.importMap.put(path, ctx);
            return true;
        } catch (IOException ex) {
            throw new IllegalImportException("Cannot import " + rawPath, ex);
        } finally {
            this.currentFile = saved;
        }
    }

    public void processLoadedModules() {
        final LinkedList<Pair<String, SiParser.TopLevelDeclContext>> queue = new LinkedList<>();
        for (final SiParser.FileContext ctx : this.importMap.values()) {
            this.processModule(queue, ctx);
        }

        // sort the queued declarations by their process priority
        queue.sort((a, b) -> Integer.compare(getProcessPriority(a.b), getProcessPriority(b.b)));

        // then process the declarations
        Pair<String, SiParser.TopLevelDeclContext> item;
        while ((item = queue.pollFirst()) != null) {
            this.namespacePrefix = item.a;
            this.visitTopLevelDecl(item.b);
        }

        InstantiatedFunction.Local ifunc;
        while ((ifunc = this.ifuncQueue.pollFirst()) != null) {
            this.processFunction(ifunc);
        }
    }

    private void processModule(List<Pair<String, SiParser.TopLevelDeclContext>> queue, SiParser.FileContext ctx) {
        // create the namespace
        this.namespacePrefix = "";
        if (ctx.ns != null) this.visitNamespaceDecl(ctx.ns);

        // then queue the declaration
        for (final SiParser.TopLevelDeclContext decl : ctx.decls) {
            queue.add(new Pair<>(this.namespacePrefix, decl));
        }
    }

    private static int getProcessPriority(SiParser.TopLevelDeclContext ctx) {
        // modules can contain three types of declarations:
        // - (local) types:    these have to be processed first
        // - native functions: these go after
        // - local functions:  these go last

        final ParseTree tree = getDeclarationContext(ctx);
        if (tree instanceof SiParser.DeclTypeContext)       return 0;
        if (tree instanceof SiParser.DeclNativeFuncContext) return 1;
        if (tree instanceof SiParser.DeclFuncContext)       return 2;

        // why not? (just in case)
        return Integer.MAX_VALUE;
    }

    private static ParseTree getDeclarationContext(SiParser.TopLevelDeclContext ctx) {
        return ctx.getChild(ctx.getChildCount() - 2);
    }

    private void processFunction(InstantiatedFunction.Local func) {
        //
    }

    @Override
    public String visitNamespacePath(SiParser.NamespacePathContext ctx) {
        final String prefix = (ctx.root == null ? this.namespacePrefix : "") + '\\';
        return ctx.parts.stream().map(Token::getText).collect(Collectors.joining("\\", prefix, ""));
    }

    @Override
    public Object visitNamespaceDecl(SiParser.NamespaceDeclContext ctx) {
        this.namespacePrefix = this.visitNamespacePath(ctx.ns);
        return null;
    }

    @Override
    public Object visitImportDecl(SiParser.ImportDeclContext ctx) {
        final String filePath = convertStringLiteral(ctx.path.getText());

        // Want to resolve it against the directory of the current file
        // that's why use resolveSibling instead of resolve
        this.loadSource(this.currentFile.resolveSibling(filePath));
        return null;
    }

    @Override
    public Object visitFile(SiParser.FileContext ctx) {
        // Only process the imports. For actual processing of declarations,
        // see methods processLoadedModules and processModule.
        for (final SiParser.ImportDeclContext importDecls : ctx.imports) {
            this.visitImportDecl(importDecls);
        }
        return null;
    }

    @Override
    public Object visitTopLevelDecl(SiParser.TopLevelDeclContext ctx) {
        this.isExported = ctx.vis != null;
        return this.visit(getDeclarationContext(ctx));
    }

    @Override
    public Object visitDeclTypeAlias(SiParser.DeclTypeAliasContext ctx) {
        System.out.println("scope =   " + this.namespacePrefix);
        System.out.println("  type:   " + ctx.name.getText());
        return null;
    }

    @Override
    public Object visitDeclNativeFunc(SiParser.DeclNativeFuncContext ctx) {
        System.out.println("scope =   " + this.namespacePrefix);
        System.out.println("  native: " + ctx.name.getText());
        return null;
    }

    @Override
    public Object visitDeclFunc(SiParser.DeclFuncContext ctx) {
        System.out.println("scope =   " + this.namespacePrefix);
        System.out.println("  func:   " + ctx.name.getText());
        return null;
    }
}
